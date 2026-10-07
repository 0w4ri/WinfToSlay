package pp.winf2slay.controller.client.event;

import pp.winf2slay.model.card.Card;

/**
 * Frage: Soll die gerade ausgespielte Karte herausgefordert werden?
 *
 * @param activePlayer Spieler, der die Karte ausgespielt hat
 * @param card ausgespielte Karte
 */
public record ChallengeRequestEvent(String activePlayer, Card card) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onChallengeRequest(this);
    }
}
