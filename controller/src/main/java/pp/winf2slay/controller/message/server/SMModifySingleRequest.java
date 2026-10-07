package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.model.die.DiceResult;

/**
 * Fragt einen Spieler, ob er einen Wurf mit einer Modifikation verändern möchte.
 */
@Serializable
public class SMModifySingleRequest extends ServerMessage {

    private DiceResult result;
    private RollPurpose purpose;
    private int threshold;
    private boolean higherWins;

    private SMModifySingleRequest() {
        // für @Serializable
    }

    /**
     * @param result aktuelles Wurfergebnis
     * @param purpose Anlass des Wurfs
     * @param threshold zu erreichender Schwellenwert
     * @param higherWins {@code true}, wenn der Wurf den Schwellenwert erreichen muss
     */
    public SMModifySingleRequest(DiceResult result, RollPurpose purpose, int threshold, boolean higherWins) {
        this.result = result;
        this.purpose = purpose;
        this.threshold = threshold;
        this.higherWins = higherWins;
    }

    /**
     * @return aktuelles Wurfergebnis
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
        return "SMModifySingleRequest[" + "result=" + result + ", " + "purpose=" + purpose + ", " + "threshold=" + threshold + ", " + "higherWins=" + higherWins + "]";
    }
}
