package pp.winf2slay.controller.client.event;

/**
 * Die Verbindung zum Server steht.
 */
public record ConnectedEvent() implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onConnected(this);
    }
}
