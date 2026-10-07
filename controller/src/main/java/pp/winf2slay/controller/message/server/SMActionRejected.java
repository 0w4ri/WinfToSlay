package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;

/**
 * Der Server hat eine Aktion abgelehnt (z. B. zu wenige Aktionspunkte).
 */
@Serializable
public class SMActionRejected extends ServerMessage {

    private String reason;
    private int actionPoints;

    private SMActionRejected() {
        // für @Serializable
    }

    /**
     * @param reason Begründung für die Anzeige
     * @param actionPoints verbleibende Aktionspunkte
     */
    public SMActionRejected(String reason, int actionPoints) {
        this.reason = reason;
        this.actionPoints = actionPoints;
    }

    /**
     * @return Begründung für die Anzeige
     */
    public String getReason() {
        return reason;
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
        return "SMActionRejected[" + "reason=" + reason + ", " + "actionPoints=" + actionPoints + "]";
    }
}
