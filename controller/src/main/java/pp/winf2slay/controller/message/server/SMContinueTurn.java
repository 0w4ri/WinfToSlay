package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;

/**
 * Die letzte Aktion ist vollständig abgehandelt; der aktive Spieler darf weiterspielen.
 */
@Serializable
public class SMContinueTurn extends ServerMessage {

    private int actionPoints;

    private SMContinueTurn() {
        // für @Serializable
    }

    /**
     * @param actionPoints verbleibende Aktionspunkte
     */
    public SMContinueTurn(int actionPoints) {
        this.actionPoints = actionPoints;
    }

    /**
     * @return verbleibende Aktionspunkte
     */
    public int getActionPoints() {
        return actionPoints;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "SMContinueTurn[" + "actionPoints=" + actionPoints + "]";
    }
}
