package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.die.DiceResult;

/**
 * Ein Wurf wurde durch eine Modifikation verändert.
 *
 * <p>Broadcast-Nachricht: Jeder Client bestätigt sie nach den Animationen.</p>
 */
@Serializable
public class BCDiceModified extends BroadcastMessage {

    private String modifier;
    private Modification modification;
    private DiceResult result;

    private BCDiceModified() {
        // für @Serializable
    }

    /**
     * @param modifier Spieler, der die Modifikation gespielt hat
     * @param modification gespielte Karte
     * @param result neues Wurfergebnis
     */
    public BCDiceModified(String modifier, Modification modification, DiceResult result) {
        this.modifier = modifier;
        this.modification = modification;
        this.result = result;
    }

    /**
     * @return Spieler, der die Modifikation gespielt hat
     */
    public String getModifier() {
        return modifier;
    }

    /**
     * @return gespielte Karte
     */
    public Modification getModification() {
        return modification;
    }

    /**
     * @return neues Wurfergebnis
     */
    public DiceResult getResult() {
        return result;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "BCDiceModified[" + "modifier=" + modifier + ", " + "modification=" + modification + ", " + "result=" + result + "]";
    }
}
