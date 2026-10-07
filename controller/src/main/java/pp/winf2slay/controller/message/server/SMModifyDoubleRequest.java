package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.die.DiceResult;

/**
 * Fragt einen Spieler, ob er einen der beiden Würfe einer Herausforderung modifizieren möchte.
 */
@Serializable
public class SMModifyDoubleRequest extends ServerMessage {

    private DiceResult activeResult;
    private DiceResult challengerResult;

    private SMModifyDoubleRequest() {
        // für @Serializable
    }

    /**
     * @param activeResult Wurf des herausgeforderten (aktiven) Spielers
     * @param challengerResult Wurf des Herausforderers
     */
    public SMModifyDoubleRequest(DiceResult activeResult, DiceResult challengerResult) {
        this.activeResult = activeResult;
        this.challengerResult = challengerResult;
    }

    /**
     * @return Wurf des herausgeforderten (aktiven) Spielers
     */
    public DiceResult getActiveResult() {
        return activeResult;
    }

    /**
     * @return Wurf des Herausforderers
     */
    public DiceResult getChallengerResult() {
        return challengerResult;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "SMModifyDoubleRequest[" + "activeResult=" + activeResult + ", " + "challengerResult=" + challengerResult + "]";
    }
}
