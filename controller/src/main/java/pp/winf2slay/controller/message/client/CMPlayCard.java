package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Card;

/**
 * Aktion: einen Helden oder Zauber ausspielen (1 Aktionspunkt).
 */
@Serializable
public class CMPlayCard extends ClientMessage {

    private Card card;

    private CMPlayCard() {
        // für @Serializable
    }

    /**
     * @param card auszuspielende Karte
     */
    public CMPlayCard(Card card) {
        this.card = card;
    }

    /**
     * @return auszuspielende Karte
     */
    public Card getCard() {
        return card;
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMPlayCard[" + "card=" + card + "]";
    }
}
