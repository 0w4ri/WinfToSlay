package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;

/**
 * Ein neuer Zug beginnt.
 */
@Serializable
public class SMTurnSwitch extends ServerMessage {

    private String activePlayer;

    private SMTurnSwitch() {
        // für @Serializable
    }

    /**
     * @param activePlayer Spieler, der jetzt am Zug ist
     */
    public SMTurnSwitch(String activePlayer) {
        this.activePlayer = activePlayer;
    }

    /**
     * @return Spieler, der jetzt am Zug ist
     */
    public String getActivePlayer() {
        return activePlayer;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "SMTurnSwitch[" + "activePlayer=" + activePlayer + "]";
    }
}
