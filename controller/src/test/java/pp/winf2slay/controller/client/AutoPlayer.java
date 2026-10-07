package pp.winf2slay.controller.client;

import pp.winf2slay.controller.client.event.ActionStateChangedEvent;
import pp.winf2slay.controller.client.event.ChallengeRequestEvent;
import pp.winf2slay.controller.client.event.GameEventListener;
import pp.winf2slay.controller.client.event.GameOverEvent;
import pp.winf2slay.controller.client.event.HeroSelectionRequestEvent;
import pp.winf2slay.controller.client.event.ModifyDoubleRequestEvent;
import pp.winf2slay.controller.client.event.ModifySingleRequestEvent;
import pp.winf2slay.controller.client.event.NoticeEvent;
import pp.winf2slay.controller.client.event.PlayerSelectionRequestEvent;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Simuliert einen menschlichen Spieler, der ausschließlich über die öffentliche
 * Schnittstelle der {@link ClientGameLogic} spielt (wie die Oberfläche).
 *
 * <p>Reaktionen werden über {@code later} verzögert ausgeführt, damit die
 * Client-Logik nie rekursiv aufgerufen wird.</p>
 */
public class AutoPlayer implements GameEventListener {

    private final ClientGameLogic logic;
    private final Consumer<Runnable> later;
    private final List<String> notices = new ArrayList<>();
    private GameOverEvent gameOver;
    private int actions;

    /**
     * @param logic Client-Logik
     * @param later führt eine Aufgabe später im selben Thread aus
     */
    public AutoPlayer(ClientGameLogic logic, Consumer<Runnable> later) {
        this.logic = logic;
        this.later = later;
        logic.getEvents().addListener(this);
    }

    /**
     * @return Spielende-Ereignis oder {@code null}
     */
    public GameOverEvent getGameOver() {
        return gameOver;
    }

    /**
     * @return Anzahl ausgeführter Aktionen
     */
    public int getActions() {
        return actions;
    }

    /**
     * @return erhaltene Hinweise
     */
    public List<String> getNotices() {
        return notices;
    }

    @Override
    public void onActionStateChanged(ActionStateChangedEvent event) {
        if (event.canAct()) later.accept(this::act);
    }

    private void act() {
        if (!logic.canAct()) return;
        actions++;
        for (Monster monster : logic.getModel().getOpenMonsters()) {
            if (monster != null && logic.canAttack(monster)) {
                logic.attackMonster(monster);
                return;
            }
        }
        for (Card card : logic.getModel().getHand()) {
            if (logic.canPlay(card)) {
                logic.playCard(card);
                return;
            }
        }
        Hero[] group = logic.getModel().getMe().orElseThrow().getGroup();
        for (Hero hero : group) {
            if (hero != null && logic.canActivate(hero)) {
                logic.activateHero(hero);
                return;
            }
        }
        if (logic.getModel().getHand().size() < 7 && logic.canDraw()) {
            logic.drawCard();
            return;
        }
        logic.endTurn();
    }

    @Override
    public void onChallengeRequest(ChallengeRequestEvent event) {
        later.accept(() -> logic.respondToChallenge(true));
    }

    @Override
    public void onModifySingleRequest(ModifySingleRequestEvent event) {
        later.accept(() -> logic.respondToModification(null));
    }

    @Override
    public void onModifyDoubleRequest(ModifyDoubleRequestEvent event) {
        later.accept(() -> logic.respondToChallengeModification(null, null));
    }

    @Override
    public void onHeroSelectionRequest(HeroSelectionRequestEvent event) {
        later.accept(() -> logic.chooseHero(event.candidates().getFirst()));
    }

    @Override
    public void onPlayerSelectionRequest(PlayerSelectionRequestEvent event) {
        later.accept(() -> logic.choosePlayer(event.candidates().getLast()));
    }

    @Override
    public void onNotice(NoticeEvent event) {
        notices.add(event.text());
    }

    @Override
    public void onGameOver(GameOverEvent event) {
        gameOver = event;
    }
}
