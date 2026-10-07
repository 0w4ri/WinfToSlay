package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.controller.message.BotLevel;

/**
 * Der Spielleiter fügt in der Lobby einen Bot hinzu.
 */
@Serializable
public class CMAddBot extends ClientMessage {

    private BotLevel level;

    private CMAddBot() {
        // für @Serializable
    }

    /**
     * @param level Spielstärke des Bots
     */
    public CMAddBot(BotLevel level) {
        this.level = level;
    }

    /**
     * @return Spielstärke des Bots
     */
    public BotLevel getLevel() {
        return level;
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMAddBot[" + "level=" + level + "]";
    }
}
