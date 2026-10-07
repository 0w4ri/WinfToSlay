package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;

/**
 * Eine Herausforderung ist entschieden.
 *
 * <p>Broadcast-Nachricht: Jeder Client bestätigt sie nach den Animationen.</p>
 */
@Serializable
public class BCChallengeResolved extends BroadcastMessage {

    private String activePlayer;
    private String challenger;
    private boolean activeWon;

    private BCChallengeResolved() {
        // für @Serializable
    }

    /**
     * @param activePlayer herausgeforderter Spieler
     * @param challenger Herausforderer
     * @param activeWon {@code true}, wenn die ausgespielte Karte bestehen bleibt
     */
    public BCChallengeResolved(String activePlayer, String challenger, boolean activeWon) {
        this.activePlayer = activePlayer;
        this.challenger = challenger;
        this.activeWon = activeWon;
    }

    /**
     * @return herausgeforderter Spieler
     */
    public String getActivePlayer() {
        return activePlayer;
    }

    /**
     * @return Herausforderer
     */
    public String getChallenger() {
        return challenger;
    }

    /**
     * @return {@code true}, wenn die ausgespielte Karte bestehen bleibt
     */
    public boolean isActiveWon() {
        return activeWon;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "BCChallengeResolved[" + "activePlayer=" + activePlayer + ", " + "challenger=" + challenger + ", " + "activeWon=" + activeWon + "]";
    }
}
