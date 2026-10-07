package pp.winf2slay.controller.client.event;

import pp.winf2slay.controller.message.server.BCCardsMoved;

/**
 * Mehrere Karten wurden gleichzeitig bewegt.
 *
 * @param move Bewegung
 */
public record CardsMovedEvent(BCCardsMoved move) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onCardsMoved(this);
    }
}
