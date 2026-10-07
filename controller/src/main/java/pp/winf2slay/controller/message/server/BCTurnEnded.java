package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;

/**
 * Ein Spieler hat seinen Zug beendet.
 *
 * <p>Broadcast-Nachricht: Jeder Client bestätigt sie nach den Animationen.</p>
 */
@Serializable
public class BCTurnEnded extends BroadcastMessage {

    private String playerName;
    private String nextPlayer;

    private BCTurnEnded() {
        // für @Serializable
    }

    /**
     * @param playerName Spieler, der seinen Zug beendet hat
     * @param nextPlayer Spieler, der als Nächstes an der Reihe ist
     */
    public BCTurnEnded(String playerName, String nextPlayer) {
        this.playerName = playerName;
        this.nextPlayer = nextPlayer;
    }

    /**
     * @return Spieler, der seinen Zug beendet hat
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * @return Spieler, der als Nächstes an der Reihe ist
     */
    public String getNextPlayer() {
        return nextPlayer;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "BCTurnEnded[" + "playerName=" + playerName + ", " + "nextPlayer=" + nextPlayer + "]";
    }
}
