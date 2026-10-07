package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Hero;

import java.util.ArrayList;
import java.util.List;

/**
 * Der aktive Spieler muss einen Helden als Ziel wählen.
 */
@Serializable
public class SMHeroSelectionRequest extends ServerMessage {

    private List<Hero> candidates;

    private SMHeroSelectionRequest() {
        // für @Serializable
    }

    /**
     * @param candidates wählbare Helden
     */
    public SMHeroSelectionRequest(List<Hero> candidates) {
        this.candidates = new ArrayList<>(candidates);
    }

    /**
     * @return wählbare Helden
     */
    public List<Hero> getCandidates() {
        return candidates;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "SMHeroSelectionRequest[" + "candidates=" + candidates + "]";
    }
}
