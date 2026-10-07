package pp.winf2slay.controller.client;

import pp.winf2slay.controller.message.client.ClientMessage;

/**
 * Versendet Nachrichten an den Server.
 */
@FunctionalInterface
public interface ClientSender {

    /**
     * @param message Nachricht an den Server
     */
    void send(ClientMessage message);
}
