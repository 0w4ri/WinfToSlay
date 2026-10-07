package pp.winf2slay.controller.message.client;

import com.jme3.network.AbstractMessage;

/**
 * Basisklasse aller Nachrichten vom Client an den Server.
 *
 * <p>Die Verarbeitung erfolgt per Double-Dispatch über
 * {@link ClientMessageInterpreter}.</p>
 */
public abstract class ClientMessage extends AbstractMessage {

    /**
     * Client-Nachrichten werden zuverlässig (TCP) übertragen.
     */
    protected ClientMessage() {
        super(true);
    }

    /**
     * Übergibt die Nachricht an den passenden {@code received}-Handler.
     *
     * @param interpreter Empfänger
     * @param from        ID des sendenden Spielers
     */
    public abstract void accept(ClientMessageInterpreter interpreter, int from);
}
