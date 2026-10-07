package pp.winf2slay.controller.client.event;

import java.util.List;

/**
 * Die Partie beginnt; der Spielzustand ist synchronisiert.
 *
 * @param seating Spieler in Sitzreihenfolge
 * @param startPlayer Spieler mit dem ersten Zug
 */
public record GameStartedEvent(List<String> seating, String startPlayer) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onGameStarted(this);
    }
}
