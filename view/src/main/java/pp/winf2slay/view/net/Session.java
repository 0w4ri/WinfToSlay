package pp.winf2slay.view.net;

import com.jme3.app.Application;
import pp.winf2slay.controller.client.AnimationGate;
import pp.winf2slay.controller.client.ClientGameLogic;
import pp.winf2slay.controller.message.server.ServerMessage;
import pp.winf2slay.controller.network.ClientNetworkListener;
import pp.winf2slay.controller.network.ClientNetworkSupport;
import pp.winf2slay.controller.network.NetworkRegistry;
import pp.winf2slay.controller.network.WinfToSlayServer;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Consumer;

/**
 * Eine Verbindung zu einem Spiel – als Gast, als Gastgeber (mit eigenem Server)
 * oder im Solospiel (eigener Server mit Bots).
 *
 * <p>Netzwerkereignisse treffen in fremden Threads ein und werden hier an den
 * jME-Render-Thread übergeben. Nach {@link #close()} werden keine Ereignisse
 * mehr weitergereicht.</p>
 */
public class Session implements ClientNetworkListener {

    private static final Logger LOGGER = System.getLogger(Session.class.getName());

    /**
     * Art der Sitzung.
     */
    public enum Mode {
        /** Allein gegen Bots (lokaler Server). */
        SOLO,
        /** Gastgeber eines Netzwerkspiels (lokaler Server). */
        HOST,
        /** Teilnehmer an einem fremden Server. */
        JOIN
    }

    /**
     * So lange darf die Anmeldung beim Server nach dem Verbindungsaufbau dauern.
     */
    private static final long HANDSHAKE_TIMEOUT_MILLIS = 8_000;

    private final Application app;
    private final Mode mode;
    private final String playerName;
    private final ClientNetworkSupport network;
    private final ClientGameLogic logic;
    private WinfToSlayServer server;
    private int port;
    private volatile boolean closed;
    private volatile boolean connected;
    private volatile boolean lost;

    /**
     * @param app        Anwendung (für die Übergabe an den Render-Thread)
     * @param mode       Art der Sitzung
     * @param playerName gewünschter Spielername
     */
    public Session(Application app, Mode mode, String playerName) {
        this.app = app;
        this.mode = mode;
        this.playerName = playerName;
        NetworkRegistry.registerAll();
        this.network = new ClientNetworkSupport(this);
        this.logic = new ClientGameLogic(network);
    }

    /**
     * @return Client-Logik
     */
    public ClientGameLogic logic() {
        return logic;
    }

    /**
     * @return Art der Sitzung
     */
    public Mode mode() {
        return mode;
    }

    /**
     * @return gewünschter Spielername
     */
    public String playerName() {
        return playerName;
    }

    /**
     * @return Port des Servers
     */
    public int port() {
        return port;
    }

    /**
     * @return lokaler Server oder {@code null}
     */
    public WinfToSlayServer server() {
        return server;
    }

    /**
     * Startet einen lokalen Server.
     *
     * @param wantedPort gewünschter Port; bei {@code 0} wird ein freier Port gesucht
     * @param botSpeed   Bedenkzeit-Faktor der Bots
     * @throws IOException wenn kein Port geöffnet werden kann
     */
    public void startServer(int wantedPort, float botSpeed) throws IOException {
        if (wantedPort > 0) {
            server = WinfToSlayServer.start(wantedPort);
            port = wantedPort;
        }
        else {
            IOException last = null;
            for (int candidate : NetUtil.candidatePorts()) {
                try {
                    server = WinfToSlayServer.start(candidate);
                    port = candidate;
                    break;
                }
                catch (IOException e) {
                    last = e;
                }
            }
            if (server == null) throw last != null ? last : new IOException("Kein freier Port gefunden");
        }
        server.setBotSpeed(botSpeed);
    }

    /**
     * Baut die Verbindung im Hintergrund auf.
     *
     * @param host    Server
     * @param port    Port
     * @param onError wird im Render-Thread mit einer Fehlermeldung aufgerufen
     */
    public void connectAsync(String host, int port, Consumer<String> onError) {
        this.port = port;
        Thread t = new Thread(() -> {
            String reason = null;
            try {
                network.connect(host, port);
                // Die TCP-Verbindung steht; jetzt muss der Server die Anmeldung bestätigen.
                long deadline = System.currentTimeMillis() + HANDSHAKE_TIMEOUT_MILLIS;
                while (!connected && !closed && !lost && System.currentTimeMillis() < deadline) {
                    Thread.sleep(50);
                }
                if (!connected && !closed && !lost) {
                    network.disconnect();
                    reason = "Der Server unter " + host + ":" + port + " antwortet nicht. "
                             + "Läuft dort WinfToSlay in derselben Version?";
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            catch (IOException | RuntimeException e) {
                LOGGER.log(Level.INFO, "Verbindung zu {0}:{1} fehlgeschlagen: {2}", host, port, e.getMessage());
                reason = "Keine Verbindung zu " + host + ":" + port + " möglich.";
            }
            if (reason != null) {
                String message = reason;
                app.enqueue(() -> {
                    if (!closed) onError.accept(message);
                });
            }
        }, "WinfToSlay-Connect");
        t.setDaemon(true);
        t.start();
    }

    /**
     * @param factor Bedenkzeit-Faktor der Bots (nur mit lokalem Server)
     */
    public void setBotSpeed(float factor) {
        if (server != null) server.setBotSpeed(factor);
    }

    /**
     * Beendet die Sitzung: Verbindung trennen und ggf. Server stoppen.
     */
    public void close() {
        if (closed) return;
        closed = true;
        logic.setAnimationGate(AnimationGate.IMMEDIATE);
        network.disconnect();
        if (server != null) server.stop();
    }

    /**
     * @return {@code true}, wenn die Sitzung beendet wurde
     */
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void onConnected() {
        connected = true;
        app.enqueue(() -> {
            if (closed) return;
            logic.connected();
            logic.joinLobby(playerName);
        });
    }

    @Override
    public void onMessage(ServerMessage message) {
        app.enqueue(() -> {
            if (!closed) logic.receive(message);
        });
    }

    @Override
    public void onDisconnected(String reason) {
        lost = true;
        app.enqueue(() -> {
            if (!closed) logic.disconnected(reason);
        });
    }
}
