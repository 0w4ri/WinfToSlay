package pp.winf2slay.controller.client.event;

import pp.winf2slay.model.card.Hero;

import java.util.List;

/**
 * Aufforderung: einen Helden als Ziel wählen.
 *
 * @param candidates wählbare Helden
 */
public record HeroSelectionRequestEvent(List<Hero> candidates) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onHeroSelectionRequest(this);
    }
}
