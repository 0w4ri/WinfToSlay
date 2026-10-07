package pp.winf2slay.view.game;

import pp.winf2slay.controller.client.ClientGameLogic;
import pp.winf2slay.controller.client.event.ChallengeRequestEvent;
import pp.winf2slay.controller.client.event.GameEventListener;
import pp.winf2slay.controller.client.event.HeroSelectionRequestEvent;
import pp.winf2slay.controller.client.event.ModifyDoubleRequestEvent;
import pp.winf2slay.controller.client.event.ModifySingleRequestEvent;
import pp.winf2slay.controller.client.event.PlayerSelectionRequestEvent;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.view.anim.Animator;
import pp.winf2slay.view.game.hud.PromptView;

import java.util.Random;

/**
 * Autopilot für Vorführungen und visuelle Tests: spielt den eigenen Platz mit
 * einer einfachen Strategie. Aktiv nur mit {@code -Dwinf.autoplay=true}.
 */
public class DemoPilot implements GameEventListener {

    private static final float THINK_SECONDS = 0.9f;

    private final ClientGameLogic logic;
    private final Animator animator;
    private final PromptView prompts;
    private final Random random = new Random();
    private Runnable pending;
    private float wait;

    /**
     * @param logic    Client-Logik
     * @param animator Animationen (der Pilot wartet, bis sie fertig sind)
     * @param prompts  Anfragen (werden beim Antworten geschlossen)
     */
    public DemoPilot(ClientGameLogic logic, Animator animator, PromptView prompts) {
        this.logic = logic;
        this.animator = animator;
        this.prompts = prompts;
        logic.getEvents().addListener(this);
    }

    /**
     * @param tpf Zeit seit dem letzten Bild
     */
    public void update(float tpf) {
        if (animator.isBusy()) {
            wait = THINK_SECONDS;
            return;
        }
        wait -= tpf;
        if (wait > 0) return;
        wait = THINK_SECONDS;
        if (pending != null) {
            Runnable r = pending;
            pending = null;
            prompts.close();
            r.run();
            return;
        }
        if (logic.canAct()) act();
    }

    private void act() {
        Monster[] open = logic.getModel().getOpenMonsters();
        if (open != null) {
            for (Monster m : open) {
                if (m != null && logic.canAttack(m)) {
                    logic.attackMonster(m);
                    return;
                }
            }
        }
        for (Card c : logic.getModel().getHand()) {
            if (logic.canPlay(c)) {
                logic.playCard(c);
                return;
            }
        }
        for (Hero h : logic.getModel().getMe().map(p -> p.getGroup()).orElse(new Hero[0])) {
            if (h != null && logic.canActivate(h)) {
                logic.activateHero(h);
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
        pending = () -> logic.respondToChallenge(random.nextInt(3) == 0);
    }

    @Override
    public void onModifySingleRequest(ModifySingleRequestEvent event) {
        pending = () -> logic.respondToModification(null);
    }

    @Override
    public void onModifyDoubleRequest(ModifyDoubleRequestEvent event) {
        pending = () -> logic.respondToChallengeModification(null, null);
    }

    @Override
    public void onHeroSelectionRequest(HeroSelectionRequestEvent event) {
        pending = () -> logic.chooseHero(event.candidates().getFirst());
    }

    @Override
    public void onPlayerSelectionRequest(PlayerSelectionRequestEvent event) {
        pending = () -> logic.choosePlayer(event.candidates().getFirst());
    }
}
