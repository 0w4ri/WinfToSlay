package pp.winf2slay.controller.client.event;

/**
 * Ein neuer Zug beginnt.
 *
 * @param player Spieler am Zug
 * @param mine {@code true}, wenn dieser Client am Zug ist
 */
public record TurnStartedEvent(String player, boolean mine) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onTurnStarted(this);
    }
}
