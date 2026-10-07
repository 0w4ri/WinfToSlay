package pp.winf2slay.controller.client.state;

import pp.winf2slay.controller.client.ClientGameLogic;
import pp.winf2slay.controller.message.server.SMGameOver;
import pp.winf2slay.controller.message.server.SMTurnSwitch;

/**
 * Das Spiel ist beendet; weitere Nachrichten werden ignoriert.
 */
public class GameOverState extends ClientState {

    /**
     * @param logic Client-Logik
     */
    public GameOverState(ClientGameLogic logic) {
        super(logic);
    }

    @Override
    public void received(SMTurnSwitch msg) {
        // nach Spielende kein Zugwechsel mehr
    }

    @Override
    public void received(SMGameOver msg) {
        // bereits beendet
    }
}
