package pp.winf2slay.controller.client.event;

/**
 * Ein Spieler hat seinen Zug beendet.
 *
 * @param player Spieler, der seinen Zug beendet hat
 * @param nextPlayer nächster Spieler
 */
public record TurnEndedEvent(String player, String nextPlayer) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onTurnEnded(this);
    }
}
