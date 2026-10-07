package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;

/**
 * Aktion: alle Handkarten abwerfen und fünf neue ziehen (3 Aktionspunkte).
 */
@Serializable
public class CMMulligan extends ClientMessage {

    /**
     * Erzeugt die Nachricht.
     */
    public CMMulligan() {
        // keine Daten
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMMulligan";
    }
}
