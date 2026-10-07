package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;

/**
 * Aktion: eine Karte ziehen (1 Aktionspunkt).
 */
@Serializable
public class CMDrawCard extends ClientMessage {

    /**
     * Erzeugt die Nachricht.
     */
    public CMDrawCard() {
        // keine Daten
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMDrawCard";
    }
}
