package pp.winf2slay.controller.client.event;

/**
 * Der Server hat die Anmeldung bestätigt.
 *
 * @param name endgültiger Spielername
 */
public record NameConfirmedEvent(String name) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onNameConfirmed(this);
    }
}
