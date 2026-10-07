package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Der aktive Spieler muss einen Spieler als Ziel wählen.
 */
@Serializable
public class SMPlayerSelectionRequest extends ServerMessage {

    private List<String> candidates;

    private SMPlayerSelectionRequest() {
        // für @Serializable
    }

    /**
     * @param candidates wählbare Spieler
     */
    public SMPlayerSelectionRequest(List<String> candidates) {
        this.candidates = new ArrayList<>(candidates);
    }

    /**
     * @return wählbare Spieler
     */
    public List<String> getCandidates() {
        return candidates;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "SMPlayerSelectionRequest[" + "candidates=" + candidates + "]";
    }
}
