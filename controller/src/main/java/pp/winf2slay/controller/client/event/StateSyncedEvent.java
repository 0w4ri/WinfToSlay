package pp.winf2slay.controller.client.event;

/**
 * Der lokale Spielzustand wurde mit dem Server abgeglichen.
 */
public record StateSyncedEvent() implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onStateSynced(this);
    }
}
