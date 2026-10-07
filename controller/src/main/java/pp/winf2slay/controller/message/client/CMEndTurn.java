package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;

/**
 * Der aktive Spieler beendet seinen Zug.
 */
@Serializable
public class CMEndTurn extends ClientMessage {

    /**
     * Erzeugt die Nachricht.
     */
    public CMEndTurn() {
        // keine Daten
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMEndTurn";
    }
}
