package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.model.die.DiceResult;

/**
 * Ein Spieler hat gewürfelt.
 *
 * <p>Broadcast-Nachricht: Jeder Client bestätigt sie nach den Animationen.</p>
 */
@Serializable
public class BCDiceRolled extends BroadcastMessage {

    private DiceResult result;
    private RollPurpose purpose;
    private int threshold;
    private boolean higherWins;

    private BCDiceRolled() {
        // für @Serializable
    }

    /**
     * @param result Wurfergebnis
     * @param purpose Anlass des Wurfs
     * @param threshold zu erreichender Schwellenwert
     * @param higherWins {@code true}, wenn der Wurf den Schwellenwert erreichen muss
     */
    public BCDiceRolled(DiceResult result, RollPurpose purpose, int threshold, boolean higherWins) {
        this.result = result;
        this.purpose = purpose;
        this.threshold = threshold;
        this.higherWins = higherWins;
    }

    /**
     * @return Wurfergebnis
     */
    public DiceResult getResult() {
        return result;
    }

    /**
     * @return Anlass des Wurfs
     */
    public RollPurpose getPurpose() {
        return purpose;
    }

    /**
     * @return zu erreichender Schwellenwert
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * @return {@code true}, wenn der Wurf den Schwellenwert erreichen muss
     */
    public boolean isHigherWins() {
        return higherWins;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "BCDiceRolled[" + "result=" + result + ", " + "purpose=" + purpose + ", " + "threshold=" + threshold + ", " + "higherWins=" + higherWins + "]";
    }
}
