package pp.winf2slay.controller.client.state;

import pp.winf2slay.controller.client.ClientGameLogic;
import pp.winf2slay.controller.message.client.CMEndTurn;
import pp.winf2slay.controller.message.client.ClientMessage;
import pp.winf2slay.model.Action;

/**
 * Der Client ist am Zug und darf eine Aktion wählen.
 */
public class MyTurnState extends ClientState {

    /**
     * @param logic Client-Logik
     */
    public MyTurnState(ClientGameLogic logic) {
        super(logic);
    }

    @Override
    public boolean canAct() {
        return true;
    }

    @Override
    public void perform(Action action, ClientMessage message) {
        model().setActionPoints(model().getActionPoints() - action.getCost());
        logic.send(message);
        logic.setState(new ActionPendingState(logic));
    }

    @Override
    public void endTurn() {
        logic.send(new CMEndTurn());
        logic.setState(new WaitState(logic));
    }
}
