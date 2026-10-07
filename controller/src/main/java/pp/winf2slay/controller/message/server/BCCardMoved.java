package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.controller.message.CardPosition;
import pp.winf2slay.controller.message.MoveCause;
import pp.winf2slay.model.card.Card;

/**
 * Eine Karte wurde bewegt.
 *
 * <p>Broadcast-Nachricht: Jeder Client bestätigt sie nach den Animationen.</p>
 */
@Serializable
public class BCCardMoved extends BroadcastMessage {

    private String actor;
    private String owner;
    private Card card;
    private CardPosition from;
    private CardPosition to;
    private MoveCause cause;

    private BCCardMoved() {
        // für @Serializable
    }

    /**
     * @param actor Spieler, der die Bewegung ausgelöst hat
     * @param owner Spieler, dessen Bereich betroffen ist
     * @param card bewegte Karte (bei verdeckten Gegner-Handkarten ggf. trotzdem gesetzt)
     * @param from Herkunft
     * @param to Ziel
     * @param cause Grund der Bewegung
     */
    public BCCardMoved(String actor, String owner, Card card, CardPosition from, CardPosition to, MoveCause cause) {
        this.actor = actor;
        this.owner = owner;
        this.card = card;
        this.from = from;
        this.to = to;
        this.cause = cause;
    }

    /**
     * @return Spieler, der die Bewegung ausgelöst hat
     */
    public String getActor() {
        return actor;
    }

    /**
     * @return Spieler, dessen Bereich betroffen ist
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @return bewegte Karte (bei verdeckten Gegner-Handkarten ggf. trotzdem gesetzt)
     */
    public Card getCard() {
        return card;
    }

    /**
     * @return Herkunft
     */
    public CardPosition getFrom() {
        return from;
    }

    /**
     * @return Ziel
     */
    public CardPosition getTo() {
        return to;
    }

    /**
     * @return Grund der Bewegung
     */
    public MoveCause getCause() {
        return cause;
    }

    /**
     * Gezogene Karten sind nur für ihren Besitzer sichtbar.
     *
     * @param viewer Name des Empfängers
     */
    @Override
    protected void hideFrom(String viewer) {
        boolean secret = from == CardPosition.SUPPORT_DECK && to == CardPosition.PLAYER_HAND;
        if (secret && !viewer.equals(owner)) card = null;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "BCCardMoved[" + "actor=" + actor + ", " + "owner=" + owner + ", " + "card=" + card + ", " + "from=" + from + ", " + "to=" + to + ", " + "cause=" + cause + "]";
    }
}
