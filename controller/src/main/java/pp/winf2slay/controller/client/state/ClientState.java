package pp.winf2slay.controller.client.state;

import pp.winf2slay.controller.client.ClientGameLogic;
import pp.winf2slay.controller.client.ClientGameState;
import pp.winf2slay.controller.client.event.CardMovedEvent;
import pp.winf2slay.controller.client.event.CardsMovedEvent;
import pp.winf2slay.controller.client.event.ChallengeModifiedEvent;
import pp.winf2slay.controller.client.event.ChallengeRequestEvent;
import pp.winf2slay.controller.client.event.ChallengeResolvedEvent;
import pp.winf2slay.controller.client.event.ChallengeRolledEvent;
import pp.winf2slay.controller.client.event.DiceModifiedEvent;
import pp.winf2slay.controller.client.event.DiceRolledEvent;
import pp.winf2slay.controller.client.event.EffectActivatedEvent;
import pp.winf2slay.controller.client.event.GameOverEvent;
import pp.winf2slay.controller.client.event.HeroSelectionRequestEvent;
import pp.winf2slay.controller.client.event.ModifyDoubleRequestEvent;
import pp.winf2slay.controller.client.event.ModifySingleRequestEvent;
import pp.winf2slay.controller.client.event.MonsterAttackedEvent;
import pp.winf2slay.controller.client.event.MulliganEvent;
import pp.winf2slay.controller.client.event.NoticeEvent;
import pp.winf2slay.controller.client.event.PlayerSelectionRequestEvent;
import pp.winf2slay.controller.client.event.RollResolvedEvent;
import pp.winf2slay.controller.client.event.TurnEndedEvent;
import pp.winf2slay.controller.client.event.TurnStartedEvent;
import pp.winf2slay.controller.message.client.ClientMessage;
import pp.winf2slay.controller.message.server.BCCardMoved;
import pp.winf2slay.controller.message.server.BCCardsMoved;
import pp.winf2slay.controller.message.server.BCChallengeModified;
import pp.winf2slay.controller.message.server.BCChallengeResolved;
import pp.winf2slay.controller.message.server.BCChallengeRolled;
import pp.winf2slay.controller.message.server.BCDiceModified;
import pp.winf2slay.controller.message.server.BCDiceRolled;
import pp.winf2slay.controller.message.server.BCEffectActivated;
import pp.winf2slay.controller.message.server.BCMonsterAttacked;
import pp.winf2slay.controller.message.server.BCMulligan;
import pp.winf2slay.controller.message.server.BCRollResolved;
import pp.winf2slay.controller.message.server.BCTurnEnded;
import pp.winf2slay.controller.message.server.SMChallengeRequest;
import pp.winf2slay.controller.message.server.SMGameOver;
import pp.winf2slay.controller.message.server.SMHeroSelectionRequest;
import pp.winf2slay.controller.message.server.SMModifyDoubleRequest;
import pp.winf2slay.controller.message.server.SMModifySingleRequest;
import pp.winf2slay.controller.message.server.SMNotice;
import pp.winf2slay.controller.message.server.SMPlayerSelectionRequest;
import pp.winf2slay.controller.message.server.SMTurnSwitch;
import pp.winf2slay.controller.message.server.ServerMessageInterpreter;
import pp.winf2slay.model.Action;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Basisklasse der Client-Zustände.
 *
 * <p>Gemeinsam für alle Zustände werden hier Spielereignisse (Broadcasts),
 * Anfragen des Servers, Hinweise, Zugwechsel und das Spielende behandelt.
 * Die Unterklassen legen fest, ob und welche Aktionen erlaubt sind.</p>
 */
public abstract class ClientState implements ServerMessageInterpreter {

    protected static final Logger LOGGER = System.getLogger(ClientState.class.getName());

    protected final ClientGameLogic logic;

    /**
     * @param logic Client-Logik
     */
    protected ClientState(ClientGameLogic logic) {
        this.logic = logic;
    }

    /**
     * @return lokaler Spielzustand
     */
    protected ClientGameState model() {
        return logic.getModel();
    }

    /**
     * Wird beim Betreten des Zustands aufgerufen.
     */
    public void entry() {
        // Standard: nichts zu tun
    }

    /**
     * @return {@code true}, wenn in diesem Zustand Aktionen erlaubt sind
     */
    public boolean canAct() {
        return false;
    }

    /**
     * Führt eine Aktion aus (nur im eigenen Zug erlaubt).
     *
     * @param action  Aktion (für die Aktionspunkte)
     * @param message Nachricht an den Server
     */
    public void perform(Action action, ClientMessage message) {
        LOGGER.log(Level.DEBUG, "{0} ist im Zustand {1} nicht erlaubt", action, this);
    }

    /**
     * Beendet den eigenen Zug (nur im eigenen Zug erlaubt).
     */
    public void endTurn() {
        LOGGER.log(Level.DEBUG, "Zugende ist im Zustand {0} nicht erlaubt", this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    // ------------------------------------------------------------------
    // Zugwechsel, Hinweise, Spielende
    // ------------------------------------------------------------------

    @Override
    public void received(SMTurnSwitch msg) {
        model().setActivePlayer(msg.getActivePlayer());
        boolean mine = model().isMyTurn();
        if (mine) model().startMyTurn();
        logic.notify(new TurnStartedEvent(msg.getActivePlayer(), mine));
        logic.setState(mine ? new MyTurnState(logic) : new WaitState(logic));
    }

    @Override
    public void received(SMNotice msg) {
        logic.notify(new NoticeEvent(msg.getText(), false));
    }

    @Override
    public void received(SMGameOver msg) {
        logic.setPendingRequest(null);
        logic.setState(new GameOverState(logic));
        boolean won = msg.getWinner() != null && msg.getWinner().equals(model().getMyName());
        logic.notify(new GameOverEvent(msg.getWinner(), msg.getReason(), won));
    }

    // ------------------------------------------------------------------
    // Anfragen
    // ------------------------------------------------------------------

    @Override
    public void received(SMChallengeRequest msg) {
        logic.setPendingRequest(msg);
        logic.notify(new ChallengeRequestEvent(msg.getActivePlayer(), msg.getCard()));
    }

    @Override
    public void received(SMModifySingleRequest msg) {
        logic.setPendingRequest(msg);
        logic.notify(new ModifySingleRequestEvent(msg.getResult(), msg.getPurpose(), msg.getThreshold(),
                                                  msg.isHigherWins()));
    }

    @Override
    public void received(SMModifyDoubleRequest msg) {
        logic.setPendingRequest(msg);
        logic.notify(new ModifyDoubleRequestEvent(msg.getActiveResult(), msg.getChallengerResult()));
    }

    @Override
    public void received(SMHeroSelectionRequest msg) {
        logic.setPendingRequest(msg);
        logic.notify(new HeroSelectionRequestEvent(msg.getCandidates()));
    }

    @Override
    public void received(SMPlayerSelectionRequest msg) {
        logic.setPendingRequest(msg);
        logic.notify(new PlayerSelectionRequestEvent(msg.getCandidates()));
    }

    // ------------------------------------------------------------------
    // Spielereignisse (Broadcasts)
    // ------------------------------------------------------------------

    @Override
    public void received(BCCardMoved msg) {
        logic.notify(new CardMovedEvent(msg));
    }

    @Override
    public void received(BCCardsMoved msg) {
        logic.notify(new CardsMovedEvent(msg));
    }

    @Override
    public void received(BCMulligan msg) {
        logic.notify(new MulliganEvent(msg));
    }

    @Override
    public void received(BCEffectActivated msg) {
        logic.notify(new EffectActivatedEvent(msg.getPlayerName(), msg.getCard()));
    }

    @Override
    public void received(BCMonsterAttacked msg) {
        logic.notify(new MonsterAttackedEvent(msg.getPlayerName(), msg.getMonster()));
    }

    @Override
    public void received(BCDiceRolled msg) {
        logic.notify(new DiceRolledEvent(msg.getResult(), msg.getPurpose(), msg.getThreshold(), msg.isHigherWins()));
    }

    @Override
    public void received(BCDiceModified msg) {
        logic.notify(new DiceModifiedEvent(msg.getModifier(), msg.getModification(), msg.getResult()));
    }

    @Override
    public void received(BCRollResolved msg) {
        logic.notify(new RollResolvedEvent(msg));
    }

    @Override
    public void received(BCChallengeRolled msg) {
        logic.notify(new ChallengeRolledEvent(msg));
    }

    @Override
    public void received(BCChallengeModified msg) {
        logic.notify(new ChallengeModifiedEvent(msg));
    }

    @Override
    public void received(BCChallengeResolved msg) {
        logic.notify(new ChallengeResolvedEvent(msg));
    }

    @Override
    public void received(BCTurnEnded msg) {
        logic.notify(new TurnEndedEvent(msg.getPlayerName(), msg.getNextPlayer()));
    }
}
