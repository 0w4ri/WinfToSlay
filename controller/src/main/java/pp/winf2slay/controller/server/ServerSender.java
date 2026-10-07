package pp.winf2slay.controller.server;

import pp.winf2slay.controller.message.server.ServerMessage;

/**
 * Versendet Nachrichten an menschliche Spieler (über das Netzwerk).
 */
public interface ServerSender {

    /**
     * Sendet eine Nachricht.
     *
     * @param playerId Verbindungs-ID des Empfängers
     * @param message  Nachricht
     */
    void send(int playerId, ServerMessage message);
}
