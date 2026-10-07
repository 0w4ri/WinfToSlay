package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Modification;

/**
 * Antwort auf die Frage, ob ein einzelner Wurf modifiziert werden soll.
 *
 * <p>Ist {@link #getModification()} {@code null}, verzichtet der Spieler.</p>
 */
@Serializable
public class CMModifySingleResponse extends ClientMessage {

    private Modification modification;

    private CMModifySingleResponse() {
        // für @Serializable
    }

    /**
     * @param modification gespielte Modifikation oder {@code null}
     */
    public CMModifySingleResponse(Modification modification) {
        this.modification = modification;
    }

    /**
     * @return gespielte Modifikation oder {@code null}
     */
    public Modification getModification() {
        return modification;
    }

    /**
     * @return Antwort „keine Modifikation“
     */
    public static CMModifySingleResponse pass() {
        return new CMModifySingleResponse(null);
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMModifySingleResponse[" + "modification=" + modification + "]";
    }
}
