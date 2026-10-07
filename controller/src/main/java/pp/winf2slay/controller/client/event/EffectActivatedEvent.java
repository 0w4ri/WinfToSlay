package pp.winf2slay.controller.client.event;

import pp.winf2slay.model.card.Card;

/**
 * Ein Held oder Zauber wird aktiviert.
 *
 * @param player aktivierender Spieler
 * @param card aktivierte Karte
 */
public record EffectActivatedEvent(String player, Card card) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onEffectActivated(this);
    }
}
