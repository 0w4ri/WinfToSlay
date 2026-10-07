package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;

/**
 * Bestätigt die Anmeldung und teilt dem Client seinen endgültigen Namen mit
 * (bei Namensgleichheit hängt der Server eine Nummer an).
 */
@Serializable
public class SMWelcome extends ServerMessage {

    private String playerName;

    private SMWelcome() {
        // für @Serializable
    }

    /**
     * @param playerName endgültiger Spielername
     */
    public SMWelcome(String playerName) {
        this.playerName = playerName;
    }

    /**
     * @return endgültiger Spielername
     */
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "SMWelcome[" + "playerName=" + playerName + "]";
    }
}
