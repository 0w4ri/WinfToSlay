package pp.winf2slay.controller.client.state;

import pp.winf2slay.controller.client.ClientGameLogic;

/**
 * Ein anderer Spieler ist am Zug. Der Client sieht zu und beantwortet
 * gegebenenfalls Anfragen (Herausforderungen, Modifikationen).
 */
public class WaitState extends ClientState {

    /**
     * @param logic Client-Logik
     */
    public WaitState(ClientGameLogic logic) {
        super(logic);
    }
}
