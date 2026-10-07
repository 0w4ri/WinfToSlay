package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;

/**
 * Antwort auf eine Spielerauswahl (Effekt „Zerstöre alle Helden eines Spielers“).
 */
@Serializable
public class CMPlayerSelectionResponse extends ClientMessage {

    private String playerName;

    private CMPlayerSelectionResponse() {
        // für @Serializable
    }

    /**
     * @param playerName gewählter Spieler
     */
    public CMPlayerSelectionResponse(String playerName) {
        this.playerName = playerName;
    }

    /**
     * @return gewählter Spieler
     */
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMPlayerSelectionResponse[" + "playerName=" + playerName + "]";
    }
}
