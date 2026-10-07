package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;

/**
 * Der Spielleiter startet das Spiel.
 */
@Serializable
public class CMStartGame extends ClientMessage {

    /**
     * Erzeugt die Nachricht.
     */
    public CMStartGame() {
        // keine Daten
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMStartGame";
    }
}
