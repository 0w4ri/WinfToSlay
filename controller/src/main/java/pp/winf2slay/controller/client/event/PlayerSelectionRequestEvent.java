package pp.winf2slay.controller.client.event;

import java.util.List;

/**
 * Aufforderung: einen Spieler als Ziel wählen.
 *
 * @param candidates wählbare Spieler
 */
public record PlayerSelectionRequestEvent(List<String> candidates) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onPlayerSelectionRequest(this);
    }
}
