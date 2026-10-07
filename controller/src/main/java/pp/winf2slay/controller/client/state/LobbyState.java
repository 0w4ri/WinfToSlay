package pp.winf2slay.controller.client.state;

import pp.winf2slay.controller.client.ClientGameLogic;
import pp.winf2slay.controller.client.event.GameStartedEvent;
import pp.winf2slay.controller.client.event.LobbyChangedEvent;
import pp.winf2slay.controller.client.event.NameConfirmedEvent;
import pp.winf2slay.controller.message.server.BCGameStarted;
import pp.winf2slay.controller.message.server.SMLobbyUpdate;
import pp.winf2slay.controller.message.server.SMWelcome;

/**
 * Lobby: Der Client hat sich verbunden und wartet auf den Spielstart.
 */
public class LobbyState extends ClientState {

    /**
     * @param logic Client-Logik
     */
    public LobbyState(ClientGameLogic logic) {
        super(logic);
    }

    @Override
    public void received(SMWelcome msg) {
        model().setMyName(msg.getPlayerName());
        logic.notify(new NameConfirmedEvent(msg.getPlayerName()));
    }

    @Override
    public void received(SMLobbyUpdate msg) {
        model().setLobby(msg.getEntries());
        logic.notify(new LobbyChangedEvent(model().getLobby(), model().isHost()));
    }

    @Override
    public void received(BCGameStarted msg) {
        logic.setState(new WaitState(logic));
        logic.notify(new GameStartedEvent(msg.getSeating(), msg.getStartPlayer()));
    }
}
