package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.die.DiceResult;

/**
 * Ein Wurf einer Herausforderung wurde modifiziert.
 *
 * <p>Broadcast-Nachricht: Jeder Client bestätigt sie nach den Animationen.</p>
 */
@Serializable
public class BCChallengeModified extends BroadcastMessage {

    private String modifier;
    private Modification modification;
    private String targetPlayer;
    private DiceResult activeResult;
    private DiceResult challengerResult;

    private BCChallengeModified() {
        // für @Serializable
    }

    /**
     * @param modifier Spieler, der die Modifikation gespielt hat
     * @param modification gespielte Karte
     * @param targetPlayer Spieler, dessen Wurf verändert wurde
     * @param activeResult aktueller Wurf des aktiven Spielers
     * @param challengerResult aktueller Wurf des Herausforderers
     */
    public BCChallengeModified(String modifier, Modification modification, String targetPlayer, DiceResult activeResult, DiceResult challengerResult) {
        this.modifier = modifier;
        this.modification = modification;
        this.targetPlayer = targetPlayer;
        this.activeResult = activeResult;
        this.challengerResult = challengerResult;
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
     * @return Spieler, dessen Wurf verändert wurde
     */
    public String getTargetPlayer() {
        return targetPlayer;
    }

    /**
     * @return aktueller Wurf des aktiven Spielers
     */
    public DiceResult getActiveResult() {
        return activeResult;
    }

    /**
     * @return aktueller Wurf des Herausforderers
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
        return "BCChallengeModified[" + "modifier=" + modifier + ", " + "modification=" + modification + ", " + "targetPlayer=" + targetPlayer + ", " + "activeResult=" + activeResult + ", " + "challengerResult=" + challengerResult + "]";
    }
}
