package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.model.card.Card;

/**
 * Ein Heldeneffekt- oder Angriffswurf ist entschieden.
 *
 * <p>Broadcast-Nachricht: Jeder Client bestätigt sie nach den Animationen.</p>
 */
@Serializable
public class BCRollResolved extends BroadcastMessage {

    private String playerName;
    private RollPurpose purpose;
    private Card target;
    private boolean success;
    private int total;

    private BCRollResolved() {
        // für @Serializable
    }

    /**
     * @param playerName würfelnder Spieler
     * @param purpose Anlass des Wurfs
     * @param target Held bzw. Monster, um das es ging
     * @param success {@code true} bei Erfolg
     * @param total Endergebnis des Wurfs
     */
    public BCRollResolved(String playerName, RollPurpose purpose, Card target, boolean success, int total) {
        this.playerName = playerName;
        this.purpose = purpose;
        this.target = target;
        this.success = success;
        this.total = total;
    }

    /**
     * @return würfelnder Spieler
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * @return Anlass des Wurfs
     */
    public RollPurpose getPurpose() {
        return purpose;
    }

    /**
     * @return Held bzw. Monster, um das es ging
     */
    public Card getTarget() {
        return target;
    }

    /**
     * @return {@code true} bei Erfolg
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return Endergebnis des Wurfs
     */
    public int getTotal() {
        return total;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "BCRollResolved[" + "playerName=" + playerName + ", " + "purpose=" + purpose + ", " + "target=" + target + ", " + "success=" + success + ", " + "total=" + total + "]";
    }
}
