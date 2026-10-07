package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;

/**
 * Ein Client meldet sich mit seinem Wunschnamen in der Lobby an.
 */
@Serializable
public class CMJoinLobby extends ClientMessage {

    private String name;

    private CMJoinLobby() {
        // für @Serializable
    }

    /**
     * @param name gewünschter Spielername
     */
    public CMJoinLobby(String name) {
        this.name = name;
    }

    /**
     * @return gewünschter Spielername
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
        return "CMJoinLobby[" + "name=" + name + "]";
    }
}
