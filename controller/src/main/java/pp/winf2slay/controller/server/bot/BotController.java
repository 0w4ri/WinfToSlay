package pp.winf2slay.controller.server.bot;

import pp.winf2slay.controller.message.BotLevel;
import pp.winf2slay.controller.message.client.CMAnimationsDone;
import pp.winf2slay.controller.message.client.CMChallengeResponse;
import pp.winf2slay.controller.message.client.CMEndTurn;
import pp.winf2slay.controller.message.client.CMHeroSelectionResponse;
import pp.winf2slay.controller.message.client.CMModifyDoubleResponse;
import pp.winf2slay.controller.message.client.CMModifySingleResponse;
import pp.winf2slay.controller.message.client.CMPlayerSelectionResponse;
import pp.winf2slay.controller.message.client.ClientMessage;
import pp.winf2slay.controller.message.server.BroadcastMessage;
import pp.winf2slay.controller.message.server.SMActionRejected;
import pp.winf2slay.controller.message.server.SMChallengeRequest;
import pp.winf2slay.controller.message.server.SMContinueTurn;
import pp.winf2slay.controller.message.server.SMGameOver;
import pp.winf2slay.controller.message.server.SMHeroSelectionRequest;
import pp.winf2slay.controller.message.server.SMModifyDoubleRequest;
import pp.winf2slay.controller.message.server.SMModifySingleRequest;
import pp.winf2slay.controller.message.server.SMPlayerSelectionRequest;
import pp.winf2slay.controller.message.server.SMTurnSwitch;
import pp.winf2slay.controller.message.server.ServerMessage;
import pp.winf2slay.controller.message.server.ServerMessageInterpreter;
import pp.winf2slay.controller.server.ServerGameLogic;
import pp.winf2slay.model.Player;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Steuert einen Bot-Spieler.
 *
 * <p>Der Controller empfängt dieselben Servernachrichten wie ein menschlicher
 * Client und antwortet mit denselben Client-Nachrichten. Die eigentlichen
 * Entscheidungen trifft die {@link BotStrategy}. Antworten werden immer über
 * den {@link pp.winf2slay.controller.server.ServerScheduler} eingeplant (nie
 * direkt), damit die Serverlogik nicht rekursiv aufgerufen wird; kleine
 * Bedenkzeiten machen die Züge der Bots für Menschen nachvollziehbar.</p>
 */
public class BotController implements ServerMessageInterpreter {

    private static final Logger LOGGER = System.getLogger(BotController.class.getName());

    /** Bedenkzeit vor einer Aktion im eigenen Zug (ms) */
    private static final long THINK_MILLIS = 900;
    /** Bedenkzeit vor einer Reaktion auf eine Anfrage (ms) */
    private static final long REACT_MILLIS = 650;
    /** Zufällige Streuung der Bedenkzeit (ms) */
    private static final long JITTER_MILLIS = 400;
    /** Nach so vielen abgelehnten Aktionen in Folge beendet der Bot seinen Zug. */
    private static final int MAX_REJECTIONS = 2;

    private final ServerGameLogic logic;
    private final Player player;
    private final BotStrategy strategy;
    private final Random random = new Random();
    private int rejections;
    private boolean finished;

    /**
     * @param logic    Serverlogik
     * @param player   gesteuerter Spieler
     * @param strategy Entscheidungslogik
     */
    BotController(ServerGameLogic logic, Player player, BotStrategy strategy) {
        this.logic = logic;
        this.player = player;
        this.strategy = strategy;
    }

    /**
     * @return Spielstärke
     */
    BotLevel getLevel() {
        return strategy.getLevel();
    }

    /**
     * Verarbeitet eine Nachricht an den Bot.
     *
     * @param msg Nachricht
     */
    void onMessage(ServerMessage msg) {
        if (finished) return;
        if (msg instanceof BroadcastMessage broadcast) {
            // Bots haben keine Animationen und bestätigen sofort (aber nie synchron).
            respond(() -> new CMAnimationsDone(broadcast.getSyncId()), 0);
        }
        msg.accept(this);
    }

    private BotContext context() {
        return new BotContext(logic.getGame(), player, logic.getTurn(), logic.getActivePlayer());
    }

    private long delay(long base) {
        double factor = logic.getBots().getSpeedFactor();
        return Math.round((base + random.nextLong(JITTER_MILLIS)) * factor);
    }

    /**
     * Plant eine Antwort ein. Die Nachricht wird erst bei Ausführung erzeugt,
     * damit die Entscheidung den dann aktuellen Spielstand berücksichtigt.
     */
    private void respond(Supplier<ClientMessage> answer, long delayMillis) {
        logic.getScheduler().schedule(() -> {
            if (finished) return;
            ClientMessage msg = answer.get();
            LOGGER.log(Level.DEBUG, "{0} antwortet {1}", player.getName(), msg);
            logic.receive(msg, player.getId());
        }, delayMillis);
    }

    // ------------------------------------------------------------------
    // Eigener Zug
    // ------------------------------------------------------------------

    private void scheduleAction() {
        respond(() -> {
            if (rejections >= MAX_REJECTIONS) return new CMEndTurn();
            return strategy.chooseAction(context());
        }, delay(THINK_MILLIS));
    }

    @Override
    public void received(SMTurnSwitch msg) {
        rejections = 0;
        if (player.getName().equals(msg.getActivePlayer())) scheduleAction();
    }

    @Override
    public void received(SMContinueTurn msg) {
        rejections = 0;
        scheduleAction();
    }

    @Override
    public void received(SMActionRejected msg) {
        rejections++;
        LOGGER.log(Level.WARNING, "Aktion von {0} abgelehnt: {1}", player.getName(), msg.getReason());
        scheduleAction();
    }

    // ------------------------------------------------------------------
    // Anfragen
    // ------------------------------------------------------------------

    @Override
    public void received(SMChallengeRequest msg) {
        respond(() -> strategy.decideChallenge(context(), msg.getActivePlayer(), msg.getCard())
                              .map(CMChallengeResponse::new)
                              .orElseGet(CMChallengeResponse::decline), delay(REACT_MILLIS));
    }

    @Override
    public void received(SMModifySingleRequest msg) {
        respond(() -> strategy.decideModification(context(), msg.getResult(), msg.getPurpose(), msg.getThreshold(),
                                                  msg.isHigherWins())
                              .map(CMModifySingleResponse::new)
                              .orElseGet(CMModifySingleResponse::pass), delay(REACT_MILLIS));
    }

    @Override
    public void received(SMModifyDoubleRequest msg) {
        respond(() -> strategy.decideChallengeModification(context(), msg.getActiveResult(), msg.getChallengerResult())
                              .map(choice -> new CMModifyDoubleResponse(choice.modification(), choice.targetPlayer()))
                              .orElseGet(CMModifyDoubleResponse::pass), delay(REACT_MILLIS));
    }

    @Override
    public void received(SMHeroSelectionRequest msg) {
        respond(() -> new CMHeroSelectionResponse(strategy.chooseHeroToDestroy(context(), msg.getCandidates())),
                delay(REACT_MILLIS));
    }

    @Override
    public void received(SMPlayerSelectionRequest msg) {
        respond(() -> new CMPlayerSelectionResponse(strategy.choosePlayerToRaid(context(), msg.getCandidates())),
                delay(REACT_MILLIS));
    }

    @Override
    public void received(SMGameOver msg) {
        finished = true;
    }
}
