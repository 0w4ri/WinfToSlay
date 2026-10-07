package pp.winf2slay.controller.network;

import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import pp.winf2slay.controller.client.ClientSender;
import pp.winf2slay.controller.message.client.ClientMessage;
import pp.winf2slay.controller.message.server.ServerMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Netzwerkverbindung eines Clients zum Server.
 */
public class ClientNetworkSupport implements MessageListener<Client>, ClientStateListener, ClientSender {

    private static final Logger LOGGER = System.getLogger(ClientNetworkSupport.class.getName());

    /**
     * Höchstdauer für den TCP-Verbindungsaufbau, bevor jME übernimmt.
     */
    public static final int CONNECT_TIMEOUT_MILLIS = 5_000;

    private final ClientNetworkListener listener;
    private volatile Client client;
    private volatile boolean closedByUs;

    /**
     * @param listener Empfänger der Netzwerkereignisse
     */
    public ClientNetworkSupport(ClientNetworkListener listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    /**
     * Baut die Verbindung auf (blockiert, daher in einem eigenen Thread aufrufen).
     * Wird währenddessen {@link #disconnect()} aufgerufen, wird die gerade
     * entstandene Verbindung sofort wieder geschlossen.
     *
     * @param host Rechnername oder IP-Adresse des Servers
     * @param port Port des Servers
     * @throws IOException           wenn keine Verbindung möglich ist
     * @throws IllegalStateException wenn bereits eine Verbindung besteht
     */
    public void connect(String host, int port) throws IOException {
        synchronized (this) {
            if (client != null)
                throw new IllegalStateException("Es besteht bereits eine Verbindung");
        }
        if (closedByUs) throw new IOException("Verbindungsaufbau abgebrochen");
        probe(host, port);
        Client c = Network.connectToServer(NetworkRegistry.GAME_NAME, NetworkRegistry.PROTOCOL_VERSION, host, port, -1);
        c.addMessageListener(this);
        c.addClientStateListener(this);
        synchronized (this) {
            client = c;
        }
        c.start();
        if (closedByUs) {
            // Abgebrochen, während die Verbindung entstand: sonst bliebe ein „Geisterspieler“ in der Lobby.
            closeQuietly(c);
            synchronized (this) {
                if (client == c) client = null;
            }
        }
    }

    /**
     * Prüft mit Zeitlimit, ob der Server erreichbar ist. jME selbst wartet beim
     * Verbindungsaufbau so lange, wie das Betriebssystem es vorgibt (bis zu Minuten).
     */
    private static void probe(String host, int port) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
        }
    }

    /**
     * @return {@code true}, wenn eine Verbindung besteht
     */
    public boolean isConnected() {
        Client c = client;
        return c != null && c.isConnected();
    }

    /**
     * Trennt die Verbindung (ohne Fehlermeldung an den Listener). Auch ein noch
     * laufender Verbindungsaufbau wird abgebrochen.
     */
    public void disconnect() {
        Client c;
        synchronized (this) {
            closedByUs = true;
            c = client;
            client = null;
        }
        if (c == null) return;
        closeQuietly(c);
        LOGGER.log(Level.INFO, "Verbindung getrennt");
    }

    private static void closeQuietly(Client c) {
        try {
            if (c.isStarted()) c.close();
        }
        catch (RuntimeException e) {
            // Der Server kann die Verbindung im selben Moment getrennt haben.
            LOGGER.log(Level.DEBUG, "Verbindung war bereits geschlossen: {0}", e.toString());
        }
    }

    @Override
    public void send(ClientMessage message) {
        Client c = client;
        if (c == null || !c.isConnected()) {
            LOGGER.log(Level.WARNING, "Keine Verbindung beim Senden von {0}", message);
            return;
        }
        try {
            c.send(message);
        }
        catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Senden von {0} fehlgeschlagen: {1}", message, e.toString());
        }
    }

    @Override
    public void messageReceived(Client source, Message message) {
        if (message instanceof ServerMessage serverMessage)
            listener.onMessage(serverMessage);
    }

    @Override
    public void clientConnected(Client source) {
        LOGGER.log(Level.INFO, "Verbunden mit dem Server");
        listener.onConnected();
    }

    @Override
    public void clientDisconnected(Client source, DisconnectInfo info) {
        LOGGER.log(Level.INFO, "Verbindung beendet: {0}", info);
        synchronized (this) {
            if (source == client) client = null;
        }
        if (!closedByUs) listener.onDisconnected(reasonOf(info));
    }

    /**
     * Übersetzt den Grund eines Verbindungsendes: Meldungen des eigenen Servers sind
     * schon deutsch, technische Gründe von jME („Connection Error“) werden ersetzt.
     */
    static String reasonOf(DisconnectInfo info) {
        if (info == null || info.reason == null) return "Die Verbindung zum Server wurde getrennt.";
        if (info.error != null || "Connection Error".equals(info.reason))
            return "Die Verbindung zum Server ist abgebrochen.";
        return info.reason;
    }
}
