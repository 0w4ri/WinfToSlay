package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Das Spiel beginnt: Anführer und Startkarten sind verteilt.
 *
 * <p>Broadcast-Nachricht: Jeder Client bestätigt sie nach den Animationen.</p>
 */
@Serializable
public class BCGameStarted extends BroadcastMessage {

    private List<String> seating;
    private String startPlayer;

    private BCGameStarted() {
        // für @Serializable
    }

    /**
     * @param seating Spieler in Sitz- und Zugreihenfolge
     * @param startPlayer Spieler mit dem ersten Zug
     */
    public BCGameStarted(List<String> seating, String startPlayer) {
        this.seating = new ArrayList<>(seating);
        this.startPlayer = startPlayer;
    }

    /**
     * @return Spieler in Sitz- und Zugreihenfolge
     */
    public List<String> getSeating() {
        return seating;
    }

    /**
     * @return Spieler mit dem ersten Zug
     */
    public String getStartPlayer() {
        return startPlayer;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "BCGameStarted[" + "seating=" + seating + ", " + "startPlayer=" + startPlayer + "]";
    }
}
