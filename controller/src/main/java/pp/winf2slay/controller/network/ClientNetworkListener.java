package pp.winf2slay.controller.network;

import pp.winf2slay.controller.message.server.ServerMessage;

/**
 * Empfänger der Netzwerkereignisse eines Clients.
 *
 * <p>Die Methoden werden in Netzwerk-Threads aufgerufen; Implementierungen
 * müssen die Verarbeitung an den passenden Thread weitergeben (in der
 * Anwendung: an den jME-Render-Thread).</p>
 */
public interface ClientNetworkListener {

    /**
     * Die Verbindung steht.
     */
    void onConnected();

    /**
     * Eine Nachricht des Servers ist eingetroffen.
     *
     * @param message Nachricht
     */
    void onMessage(ServerMessage message);

    /**
     * Die Verbindung wurde getrennt.
     *
     * @param reason Begründung
     */
    void onDisconnected(String reason);
}
