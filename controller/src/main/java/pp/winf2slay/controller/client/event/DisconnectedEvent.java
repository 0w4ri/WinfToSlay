package pp.winf2slay.controller.client.event;

/**
 * Die Verbindung zum Server wurde getrennt.
 *
 * @param reason Begründung für die Anzeige
 */
public record DisconnectedEvent(String reason) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onDisconnected(this);
    }
}
