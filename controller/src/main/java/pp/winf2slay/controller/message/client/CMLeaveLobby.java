package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;

/**
 * Ein Client verlässt die Lobby.
 */
@Serializable
public class CMLeaveLobby extends ClientMessage {

    /**
     * Erzeugt die Nachricht.
     */
    public CMLeaveLobby() {
        // keine Daten
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMLeaveLobby";
    }
}
