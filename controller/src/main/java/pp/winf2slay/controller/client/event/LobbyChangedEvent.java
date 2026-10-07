package pp.winf2slay.controller.client.event;

import pp.winf2slay.controller.message.LobbyEntry;

import java.util.List;

/**
 * Die Spielerliste der Lobby hat sich geändert.
 *
 * @param entries Spieler in Sitzreihenfolge
 * @param host {@code true}, wenn dieser Client die Partie leitet
 */
public record LobbyChangedEvent(List<LobbyEntry> entries, boolean host) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onLobbyChanged(this);
    }
}
