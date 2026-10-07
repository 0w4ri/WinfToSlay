package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;

/**
 * Der Spielleiter entfernt einen Bot aus der Lobby.
 */
@Serializable
public class CMRemoveBot extends ClientMessage {

    private String name;

    private CMRemoveBot() {
        // für @Serializable
    }

    /**
     * @param name Name des Bots
     */
    public CMRemoveBot(String name) {
        this.name = name;
    }

    /**
     * @return Name des Bots
     */
    public String getName() {
        return name;
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMRemoveBot[" + "name=" + name + "]";
    }
}
