package pp.winf2slay.controller.client.event;

/**
 * Eine offene Anfrage wurde beantwortet; Dialoge können geschlossen werden.
 */
public record RequestClosedEvent() implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onRequestClosed(this);
    }
}
