package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.controller.message.LobbyEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Aktuelle Spielerliste der Lobby.
 */
@Serializable
public class SMLobbyUpdate extends ServerMessage {

    private List<LobbyEntry> entries;

    private SMLobbyUpdate() {
        // für @Serializable
    }

    /**
     * @param entries Spieler in Sitzreihenfolge
     */
    public SMLobbyUpdate(List<LobbyEntry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    /**
     * @return Spieler in Sitzreihenfolge
     */
    public List<LobbyEntry> getEntries() {
        return entries;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "SMLobbyUpdate[" + "entries=" + entries + "]";
    }
}
