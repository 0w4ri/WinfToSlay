package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;

/**
 * Der Client hat alle Animationen zu einer Broadcast-Nachricht abgespielt.
 * Erst wenn alle Clients dies bestätigt haben, setzt der Server das Spiel fort.
 */
@Serializable
public class CMAnimationsDone extends ClientMessage {

    private int syncId;

    private CMAnimationsDone() {
        // für @Serializable
    }

    /**
     * @param syncId Kennung der bestätigten Broadcast-Nachricht
     */
    public CMAnimationsDone(int syncId) {
        this.syncId = syncId;
    }

    /**
     * @return Kennung der bestätigten Broadcast-Nachricht
     */
    public int getSyncId() {
        return syncId;
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMAnimationsDone[" + "syncId=" + syncId + "]";
    }
}
