package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Modification;

/**
 * Antwort auf die Frage, ob einer der beiden Würfe einer Herausforderung modifiziert werden soll.
 *
 * <p>Ist {@link #getModification()} {@code null}, verzichtet der Spieler.</p>
 */
@Serializable
public class CMModifyDoubleResponse extends ClientMessage {

    private Modification modification;
    private String targetPlayer;

    private CMModifyDoubleResponse() {
        // für @Serializable
    }

    /**
     * @param modification gespielte Modifikation oder {@code null}
     * @param targetPlayer Spieler, dessen Wurf verändert wird
     */
    public CMModifyDoubleResponse(Modification modification, String targetPlayer) {
        this.modification = modification;
        this.targetPlayer = targetPlayer;
    }

    /**
     * @return gespielte Modifikation oder {@code null}
     */
    public Modification getModification() {
        return modification;
    }

    /**
     * @return Spieler, dessen Wurf verändert wird
     */
    public String getTargetPlayer() {
        return targetPlayer;
    }

    /**
     * @return Antwort „keine Modifikation“
     */
    public static CMModifyDoubleResponse pass() {
        return new CMModifyDoubleResponse(null, null);
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMModifyDoubleResponse[" + "modification=" + modification + ", " + "targetPlayer=" + targetPlayer + "]";
    }
}
