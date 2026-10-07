package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;

/**
 * Das Spiel ist beendet.
 */
@Serializable
public class SMGameOver extends ServerMessage {

    private String winner;
    private String reason;

    private SMGameOver() {
        // für @Serializable
    }

    /**
     * @param winner Gewinner oder {@code null}, wenn das Spiel abgebrochen wurde
     * @param reason Begründung für die Anzeige
     */
    public SMGameOver(String winner, String reason) {
        this.winner = winner;
        this.reason = reason;
    }

    /**
     * @return Gewinner oder {@code null}, wenn das Spiel abgebrochen wurde
     */
    public String getWinner() {
        return winner;
    }

    /**
     * @return Begründung für die Anzeige
     */
    public String getReason() {
        return reason;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "SMGameOver[" + "winner=" + winner + ", " + "reason=" + reason + "]";
    }
}
