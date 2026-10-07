package pp.winf2slay.controller.network;

import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializer;
import pp.winf2slay.controller.message.client.ClientMessage;
import pp.winf2slay.controller.message.server.ServerMessage;
import pp.winf2slay.controller.server.ServerGameLogic;
import pp.winf2slay.controller.server.ServerScheduler;
import pp.winf2slay.controller.server.ServerSender;
import pp.winf2slay.model.Game;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Netzwerkserver von WinfToSlay.
 *
 * <p>Alle Ereignisse (Nachrichten, Verbindungen, Bot-Aktionen, Zeitüberschreitungen)
 * werden in eine Warteschlange gestellt und nacheinander in einem eigenen
 * Server-Thread abgearbeitet. Die {@link ServerGameLogic} wird so nie von zwei
 * Threads gleichzeitig benutzt.</p>
 */
public class WinfToSlayServer implements MessageListener<HostedConnection>, ConnectionListener, ServerSender,
                                         ServerScheduler {

    private static final Logger LOGGER = System.getLogger(WinfToSlayServer.class.getName());

    /**
     * Standardport.
     */
    public static final int DEFAULT_PORT = 1234;

    /**
     * Nach Spielende läuft der Server noch so lange weiter, damit alle Clients
     * die letzte Nachricht sicher erhalten.
     */
    private static final long SHUTDOWN_DELAY_MILLIS = 3_000;

    /**
     * So lange wartet {@link #stop()}, bis alle Verbindungen sauber geschlossen sind.
     */
    private static final long CLOSE_TIMEOUT_MILLIS = 1_000;

    /**
     * Abgewiesene Verbindungen werden erst nach dieser Zeit geschlossen: jME meldet eine neue
     * Verbindung, bevor es dem Client seine Registrierung schickt. Ein sofortiges Schließen
     * führt dort zu einer {@code KernelException} im Protokoll.
     */
    private static final long REJECT_DELAY_MILLIS = 300;

    /**
     * Begründung für abgewiesene Verbindungen.
     */
    static final String REJECT_REASON = "Das Spiel ist voll oder läuft bereits.";

    private final Server server;
    private final ServerGameLogic logic;
    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService timer;
    private final Thread thread;
    private volatile boolean running;
    /**
     * Abgewiesene Verbindungen (nur im Server-Thread benutzt): ihre Nachrichten werden ignoriert.
     */
    private final Set<Integer> rejected = new HashSet<>();

    private WinfToSlayServer(int port, Game game) throws IOException {
        NetworkRegistry.registerAll();
        // jME sperrt das Serializer-Register beim Start eines Servers. Ein zweiter Server im
        // selben Programm (z. B. „Nochmal spielen“) registriert seine Standardnachrichten
        // erneut und scheitert sonst – die Registrierung ist idempotent, also wieder öffnen.
        synchronized (NetworkRegistry.class) {
            Serializer.setReadOnly(false);
        }
        // Nur TCP: Alle Nachrichten sind zuverlässig, und ein verlorenes UDP-Paket beim
        // Verbindungsaufbau (Firewall, WLAN) würde den Beitritt sonst endlos hängen lassen.
        server = Network.createServer(NetworkRegistry.GAME_NAME, NetworkRegistry.PROTOCOL_VERSION, port, -1);
        logic = new ServerGameLogic(this, this, game);
        logic.setOnGameFinished(() -> schedule(this::stop, SHUTDOWN_DELAY_MILLIS));
        timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WinfToSlay-Server-Timer");
            t.setDaemon(true);
            return t;
        });
        thread = new Thread(this::processTasks, "WinfToSlay-Server");
        thread.setDaemon(true);
    }

    /**
     * Startet einen Server mit neuem, zufällig gemischtem Spiel.
     *
     * @param port Port
     * @return laufender Server
     * @throws IOException wenn der Port nicht geöffnet werden kann
     */
    public static WinfToSlayServer start(int port) throws IOException {
        return start(port, new Game());
    }

    /**
     * Startet einen Server für ein vorgegebenes Spiel.
     *
     * @param port Port
     * @param game Spiel
     * @return laufender Server
     * @throws IOException wenn der Port nicht geöffnet werden kann
     */
    public static WinfToSlayServer start(int port, Game game) throws IOException {
        WinfToSlayServer s = new WinfToSlayServer(port, game);
        NetworkRegistry.CLIENT_MESSAGES.forEach(type -> s.server.addMessageListener(s, type));
        s.server.addConnectionListener(s);
        try {
            // Der Port wird erst hier geöffnet; ist er belegt, wirft jME eine KernelException.
            s.server.start();
        }
        catch (RuntimeException e) {
            s.timer.shutdownNow();
            throw new IOException("Port " + port + " kann nicht geöffnet werden", e);
        }
        s.running = true;
        s.thread.start();
        LOGGER.log(Level.INFO, "Server läuft auf Port {0}", port);
        return s;
    }

    /**
     * Setzt den Faktor für die Bedenkzeit der Bots (thread-sicher).
     *
     * @param factor 1 = normal, kleiner = schneller
     */
    public void setBotSpeed(double factor) {
        logic.getBots().setSpeedFactor(factor);
    }

    /**
     * @return {@code true}, solange der Server läuft
     */
    public boolean isRunning() {
        return running;
    }

    private void processTasks() {
        while (running) {
            try {
                Runnable task = tasks.poll(200, TimeUnit.MILLISECONDS);
                if (task != null) task.run();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            catch (RuntimeException e) {
                // Ein Fehler in einer einzelnen Nachricht darf den Server nicht beenden.
                LOGGER.log(Level.ERROR, "Fehler im Server-Thread", e);
            }
        }
    }

    /**
     * Beendet den Server und trennt alle Verbindungen.
     */
    public void stop() {
        // Kann gleichzeitig vom Timer (nach Spielende) und vom Render-Thread kommen.
        synchronized (this) {
            if (!running) return;
            running = false;
        }
        LOGGER.log(Level.INFO, "Server wird beendet");
        try {
            for (HostedConnection client : server.getConnections()) {
                client.close("Der Gastgeber hat das Spiel beendet.");
            }
            // Das Schließen läuft im Netzwerk-Thread von jME. Würde der Server sofort beendet,
            // blieben die Verbindungen offen und die Mitspieler erführen nie vom Ende.
            long deadline = System.currentTimeMillis() + CLOSE_TIMEOUT_MILLIS;
            while (!server.getConnections().isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            server.close();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            server.close();
        }
        catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Fehler beim Beenden des Servers: {0}", e.toString());
        }
        timer.shutdownNow();
        thread.interrupt();
    }

    // ------------------------------------------------------------------
    // Netzwerk-Callbacks (laufen in jME-Netzwerkthreads)
    // ------------------------------------------------------------------

    @Override
    public void messageReceived(HostedConnection source, Message message) {
        if (message instanceof ClientMessage clientMessage) {
            int from = source.getId();
            tasks.add(() -> {
                if (!rejected.contains(from)) logic.receive(clientMessage, from);
            });
        }
    }

    @Override
    public void connectionAdded(Server server, HostedConnection connection) {
        LOGGER.log(Level.INFO, "Neue Verbindung {0} von {1}", connection.getId(), connection.getAddress());
        int id = connection.getId();
        tasks.add(() -> {
            if (!logic.connectionAdded(id)) {
                rejected.add(id);
                schedule(() -> closeQuietly(connection, REJECT_REASON), REJECT_DELAY_MILLIS);
            }
        });
    }

    @Override
    public void connectionRemoved(Server server, HostedConnection connection) {
        LOGGER.log(Level.INFO, "Verbindung {0} getrennt", connection.getId());
        int id = connection.getId();
        tasks.add(() -> {
            // Abgewiesene Verbindungen kennt die Spiellogik nicht.
            if (!rejected.remove(id)) logic.connectionRemoved(id);
        });
    }

    private static void closeQuietly(HostedConnection connection, String reason) {
        try {
            connection.close(reason);
        }
        catch (RuntimeException e) {
            // Der Client kann die Verbindung inzwischen selbst getrennt haben.
            LOGGER.log(Level.DEBUG, "Verbindung {0} war bereits geschlossen: {1}", connection.getId(), e.toString());
        }
    }

    // ------------------------------------------------------------------
    // ServerSender & ServerScheduler (laufen im Server-Thread)
    // ------------------------------------------------------------------

    @Override
    public void send(int playerId, ServerMessage message) {
        HostedConnection connection = server.getConnection(playerId);
        if (connection != null && running)
            connection.send(message);
        else
            LOGGER.log(Level.DEBUG, "keine Verbindung {0} für {1}", playerId, message);
    }

    @Override
    public void schedule(Runnable task, long delayMillis) {
        if (delayMillis <= 0) {
            tasks.add(task);
        }
        else if (!timer.isShutdown()) {
            timer.schedule(() -> tasks.add(task), delayMillis, TimeUnit.MILLISECONDS);
        }
    }
}
