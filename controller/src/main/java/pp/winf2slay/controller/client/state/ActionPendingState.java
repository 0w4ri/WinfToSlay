package pp.winf2slay.controller.client.state;

import pp.winf2slay.controller.client.ClientGameLogic;
import pp.winf2slay.controller.client.event.NoticeEvent;
import pp.winf2slay.controller.message.server.SMActionRejected;
import pp.winf2slay.controller.message.server.SMContinueTurn;

/**
 * Der Client hat eine Aktion ausgeführt und wartet, bis der Server sie
 * vollständig abgehandelt hat (Herausforderungen, Würfe, Effekte).
 */
public class ActionPendingState extends ClientState {

    /**
     * @param logic Client-Logik
     */
    public ActionPendingState(ClientGameLogic logic) {
        super(logic);
    }

    @Override
    public void received(SMContinueTurn msg) {
        model().setActionPoints(msg.getActionPoints());
        logic.setState(new MyTurnState(logic));
    }

    @Override
    public void received(SMActionRejected msg) {
        model().setActionPoints(msg.getActionPoints());
        logic.notify(new NoticeEvent(msg.getReason(), true));
        logic.setState(new MyTurnState(logic));
    }
}
