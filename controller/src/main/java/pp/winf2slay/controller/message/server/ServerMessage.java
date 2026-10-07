package pp.winf2slay.controller.message.server;

import com.jme3.network.AbstractMessage;

/**
 * Basisklasse aller Nachrichten vom Server an die Clients.
 *
 * <p>Die Verarbeitung erfolgt per Double-Dispatch über
 * {@link ServerMessageInterpreter}.</p>
 */
public abstract class ServerMessage extends AbstractMessage {

    /**
     * Server-Nachrichten werden zuverlässig (TCP) übertragen.
     */
    protected ServerMessage() {
        super(true);
    }

    /**
     * Übergibt die Nachricht an den passenden {@code received}-Handler.
     *
     * @param interpreter Empfänger
     */
    public abstract void accept(ServerMessageInterpreter interpreter);
}
