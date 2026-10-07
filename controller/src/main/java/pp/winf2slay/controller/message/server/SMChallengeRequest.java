package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Card;

/**
 * Fragt einen Spieler, ob er die gerade ausgespielte Karte herausfordern möchte.
 */
@Serializable
public class SMChallengeRequest extends ServerMessage {

    private String activePlayer;
    private Card card;

    private SMChallengeRequest() {
        // für @Serializable
    }

    /**
     * @param activePlayer Spieler, der die Karte ausgespielt hat
     * @param card ausgespielte Karte
     */
    public SMChallengeRequest(String activePlayer, Card card) {
        this.activePlayer = activePlayer;
        this.card = card;
    }

    /**
     * @return Spieler, der die Karte ausgespielt hat
     */
    public String getActivePlayer() {
        return activePlayer;
    }

    /**
     * @return ausgespielte Karte
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
        return "SMChallengeRequest[" + "activePlayer=" + activePlayer + ", " + "card=" + card + "]";
    }
}
