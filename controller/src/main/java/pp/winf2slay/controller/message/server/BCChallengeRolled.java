package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.die.DiceResult;

/**
 * Beide Seiten einer Herausforderung haben gewürfelt.
 *
 * <p>Broadcast-Nachricht: Jeder Client bestätigt sie nach den Animationen.</p>
 */
@Serializable
public class BCChallengeRolled extends BroadcastMessage {

    private String activePlayer;
    private DiceResult activeResult;
    private String challenger;
    private DiceResult challengerResult;

    private BCChallengeRolled() {
        // für @Serializable
    }

    /**
     * @param activePlayer herausgeforderter (aktiver) Spieler
     * @param activeResult Wurf des aktiven Spielers
     * @param challenger Herausforderer
     * @param challengerResult Wurf des Herausforderers
     */
    public BCChallengeRolled(String activePlayer, DiceResult activeResult, String challenger, DiceResult challengerResult) {
        this.activePlayer = activePlayer;
        this.activeResult = activeResult;
        this.challenger = challenger;
        this.challengerResult = challengerResult;
    }

    /**
     * @return herausgeforderter (aktiver) Spieler
     */
    public String getActivePlayer() {
        return activePlayer;
    }

    /**
     * @return Wurf des aktiven Spielers
     */
    public DiceResult getActiveResult() {
        return activeResult;
    }

    /**
     * @return Herausforderer
     */
    public String getChallenger() {
        return challenger;
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
        return "BCChallengeRolled[" + "activePlayer=" + activePlayer + ", " + "activeResult=" + activeResult + ", " + "challenger=" + challenger + ", " + "challengerResult=" + challengerResult + "]";
    }
}
