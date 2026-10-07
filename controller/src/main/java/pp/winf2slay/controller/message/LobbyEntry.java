package pp.winf2slay.controller.message;

import com.jme3.network.serializing.Serializable;

/**
 * Ein Eintrag der Spielerliste in der Lobby.
 */
@Serializable
public class LobbyEntry {

    private String name;
    private boolean bot;
    private boolean host;
    private BotLevel level;

    private LobbyEntry() {
        // für @Serializable
    }

    /**
     * @param name  Spielername
     * @param bot   {@code true} für Bots
     * @param host  {@code true} für den Spieler, der das Spiel eröffnet hat
     * @param level Spielstärke (nur bei Bots, sonst {@code null})
     */
    public LobbyEntry(String name, boolean bot, boolean host, BotLevel level) {
        this.name = name;
        this.bot = bot;
        this.host = host;
        this.level = level;
    }

    /**
     * @return Spielername
     */
    public String getName() {
        return name;
    }

    /**
     * @return {@code true} für Bots
     */
    public boolean isBot() {
        return bot;
    }

    /**
     * @return {@code true} für den Spielleiter
     */
    public boolean isHost() {
        return host;
    }

    /**
     * @return Spielstärke eines Bots; {@code null} bei Menschen
     */
    public BotLevel getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return name + (bot ? " [Bot " + level + "]" : "") + (host ? " [Host]" : "");
    }
}
