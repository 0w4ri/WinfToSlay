package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;

/**
 * Hinweis für die Anzeige, z. B. „Anna hat das Spiel verlassen – ein Bot übernimmt.“
 */
@Serializable
public class SMNotice extends ServerMessage {

    private String text;

    private SMNotice() {
        // für @Serializable
    }

    /**
     * @param text anzuzeigender Text
     */
    public SMNotice(String text) {
        this.text = text;
    }

    /**
     * @return anzuzeigender Text
     */
    public String getText() {
        return text;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "SMNotice[" + "text=" + text + "]";
    }
}
