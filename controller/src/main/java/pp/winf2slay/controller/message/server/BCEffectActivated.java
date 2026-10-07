package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Card;

/**
 * Ein Held oder Zauber wird aktiviert (Helden würfeln anschließend).
 *
 * <p>Broadcast-Nachricht: Jeder Client bestätigt sie nach den Animationen.</p>
 */
@Serializable
public class BCEffectActivated extends BroadcastMessage {

    private String playerName;
    private Card card;

    private BCEffectActivated() {
        // für @Serializable
    }

    /**
     * @param playerName aktivierender Spieler
     * @param card aktivierte Karte
     */
    public BCEffectActivated(String playerName, Card card) {
        this.playerName = playerName;
        this.card = card;
    }

    /**
     * @return aktivierender Spieler
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * @return aktivierte Karte
     */
    public Card getCard() {
        return card;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "BCEffectActivated[" + "playerName=" + playerName + ", " + "card=" + card + "]";
    }
}
