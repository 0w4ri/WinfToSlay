package pp.winf2slay.controller.client.event;

import pp.winf2slay.controller.message.server.BCCardMoved;

/**
 * Eine Karte wurde bewegt.
 *
 * @param move Bewegung
 */
public record CardMovedEvent(BCCardMoved move) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onCardMoved(this);
    }
}
