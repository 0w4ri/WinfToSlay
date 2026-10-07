package pp.winf2slay.view.game;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import pp.winf2slay.controller.client.AnimationGate;
import pp.winf2slay.controller.client.ClientGameLogic;
import pp.winf2slay.controller.client.ClientGameState;
import pp.winf2slay.controller.client.event.DisconnectedEvent;
import pp.winf2slay.controller.client.event.GameEventListener;
import pp.winf2slay.controller.client.event.GameOverEvent;
import pp.winf2slay.controller.client.event.GameStartedEvent;
import pp.winf2slay.model.Action;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.card.Spell;
import pp.winf2slay.model.field.Group;
import pp.winf2slay.view.WinfToSlayApp;
import pp.winf2slay.view.anim.Animator;
import pp.winf2slay.view.anim.Tweens;
import pp.winf2slay.view.audio.Sfx;
import pp.winf2slay.view.fx.Fx;
import pp.winf2slay.view.game.board.BoardView;
import pp.winf2slay.view.game.board.BoardView.Place;
import pp.winf2slay.view.game.board.SeatView;
import pp.winf2slay.view.game.card.CardFactory;
import pp.winf2slay.view.game.card.CardNode;
import pp.winf2slay.view.game.card.HandView;
import pp.winf2slay.view.game.hud.CardInspector;
import pp.winf2slay.view.game.hud.DicePanel;
import pp.winf2slay.view.game.hud.Hud;
import pp.winf2slay.view.game.hud.PromptView;
import pp.winf2slay.view.net.Session;
import pp.winf2slay.view.settings.UserSettings;
import pp.winf2slay.view.ui.ButtonVariant;
import pp.winf2slay.view.ui.Dialog;
import pp.winf2slay.view.ui.Theme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Die laufende Partie: 3D-Tisch, Hand, Oberfläche, Eingaben und Regie.
 *
 * <p>Der Bildschirm wird beim Spielstart von der Lobby aus erzeugt und mit
 * {@link #start(GameStartedEvent)} sofort aufgebaut – noch bevor die
 * Bestätigung des Startereignisses an den Server geht. So wartet der Server auf
 * die Eröffnungsanimation.</p>
 */
public class GameScreen extends BaseAppState implements GameEventListener, Hud.Actions, HandView.Listener,
                                                       BoardView.Interaction {

    private static final String M_HELP = "game.help";
    private static final String M_BOOST = "game.boost";
    private static final String M_RESET = "game.reset";
    private static final String M_DRAW = "game.draw";
    private static final String M_END = "game.end";
    private static final String M_ZOOM_IN = "game.zoomIn";
    private static final String M_ZOOM_OUT = "game.zoomOut";
    private static final String M_ORBIT = "game.orbit";
    private static final String M_LEFT = "game.left";
    private static final String M_RIGHT = "game.right";
    private static final String M_UP = "game.up";
    private static final String M_DOWN = "game.down";
    // Pfeiltasten schwenken die Kamera – für Trackpads ohne mittlere Maustaste (macOS)
    private static final String M_KEY_LEFT = "game.keyLeft";
    private static final String M_KEY_RIGHT = "game.keyRight";
    private static final String M_KEY_UP = "game.keyUp";
    private static final String M_KEY_DOWN = "game.keyDown";
    private static final String[] MAPPINGS = {M_HELP, M_BOOST, M_RESET, M_DRAW, M_END, M_ZOOM_IN, M_ZOOM_OUT,
                                              M_ORBIT, M_LEFT, M_RIGHT, M_UP, M_DOWN, M_KEY_LEFT, M_KEY_RIGHT,
                                              M_KEY_UP, M_KEY_DOWN};

    private final WinfToSlayApp app;
    private final Session session;
    private final ClientGameLogic logic;
    private final Node gui = new Node("game-gui");
    private final Node overlay = new Node("game-overlay");
    private final Set<Hero> selectable = new HashSet<>();
    private final InputHandler input = new InputHandler();
    private Stage stage;
    private GameDirector director;
    private CardInspector inspector;
    private DemoPilot pilot;
    private Dialog menu;
    private boolean orbiting;
    private boolean finished;
    private boolean disposed;
    private CardNode hovered;

    /**
     * @param app     Anwendung
     * @param session Sitzung
     */
    public GameScreen(WinfToSlayApp app, Session session) {
        this.app = app;
        this.session = session;
        this.logic = session.logic();
    }

    /**
     * Baut die Spielansicht auf und startet die Eröffnung.
     *
     * @param event Startereignis
     */
    public void start(GameStartedEvent event) {
        ClientGameState model = logic.getModel();
        List<String> seating = rotate(event.seating(), model.getMyName());
        Animator animator = app.getAnimator();
        animator.clear();
        CardFactory cards = app.getCards();
        TableScene table = app.getTable();
        app.getGuiNode().attachChild(gui);
        app.getGuiNode().attachChild(overlay);
        overlay.setLocalTranslation(0, 0, 0);

        BoardView board = new BoardView(cards, table.getBoard(), seating, animator, this);
        HandView hand = new HandView(cards, gui, app.getCamera(), this);
        Hud hud = new Hud(app.getUi(), app, animator, cards, this);
        hud.build(gui, seating);
        DicePanel dicePanel = new DicePanel(app.getUi(), app.getCamera(), gui);
        PromptView prompts = new PromptView(app.getUi(), app, animator, cards, gui);
        Fx fx = new Fx(app.getAssetManager(), table.getEffects(), overlay, app.getCamera(), app.getRig(),
                       app.getSounds(), animator, app.getUi().theme());
        fx.setDensity(app.getSettings().getQuality() == UserSettings.Quality.HIGH ? 1f : 0.5f);
        DiceView dice = new DiceView(app.getAssetManager(), table.getEffects(), app.getSounds(), fx);
        TurnToken token = new TurnToken(app.getAssetManager(), table.getBoard(), board.layout());
        stage = new Stage(logic, board, hand, hud, dicePanel, dice, prompts, fx, app.getRig(), app.getSounds(),
                          animator, new ScreenBridge(app.getCamera()), token);
        inspector = new CardInspector(app.getUi(), app, cards);
        director = new GameDirector(stage, this::refresh, this::setHeroSelection, this::showGameOver);

        board.build(model);
        hand.sync(List.of());
        board.setActive(model.getActivePlayer());
        hud.refresh(model, logic);
        logic.getEvents().addListener(director);
        logic.getEvents().addListener(this);
        logic.setAnimationGate(gate());
        director.intro(event.startPlayer());
        if (Boolean.getBoolean("winf.autoplay")) pilot = new DemoPilot(logic, animator, prompts);
        app.getSounds().setMusicLevel(0.55f);
    }

    private AnimationGate gate() {
        return done -> stage.animator().whenIdle(() -> {
            if (!finished && stage != null) {
                ClientGameState model = logic.getModel();
                stage.board().reconcile(model);
                stage.hand().sync(model.getHand());
                refresh();
            }
            done.run();
        });
    }

    private static List<String> rotate(List<String> seating, String me) {
        int index = Math.max(0, seating.indexOf(me));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < seating.size(); i++) result.add(seating.get((index + i) % seating.size()));
        return result;
    }

    // ------------------------------------------------------------------
    // Lebenszyklus
    // ------------------------------------------------------------------

    @Override
    protected void initialize(Application application) {
        // Aufbau geschieht in start()
    }

    @Override
    protected void cleanup(Application application) {
        dispose();
    }

    /**
     * @return Bausteine der Spielansicht oder {@code null} vor dem Start (für Entwicklerwerkzeuge)
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * @return {@code true}, solange die Großansicht einer Karte offen ist (für Entwicklerwerkzeuge)
     */
    public boolean isInspecting() {
        return inspector != null && inspector.isOpen();
    }

    /**
     * Räumt die Spielansicht ab (idempotent). Wird auch direkt beim Verlassen aufgerufen,
     * weil jME {@link #cleanup(Application)} nicht aufruft, wenn der Zustand vor seiner
     * Initialisierung wieder entfernt wird (z. B. Abbruch direkt nach dem Spielstart).
     */
    public void dispose() {
        if (disposed) return;
        disposed = true;
        logic.getEvents().removeListener(director);
        logic.getEvents().removeListener(this);
        logic.setAnimationGate(AnimationGate.IMMEDIATE);
        if (inspector != null) inspector.close();
        if (menu != null) menu.close();
        if (stage != null) {
            stage.animator().clear();
            stage.dice().clearNow();
            stage.prompts().detach();
            stage.dicePanel().detach();
            stage.hand().detach();
            stage.hud().detach();
            stage.board().clear();
            app.getTable().getEffects().detachAllChildren();
            stage.rig().release();
        }
        gui.removeFromParent();
        overlay.removeFromParent();
        overlay.detachAllChildren();
        app.getSounds().setMusicLevel(1f);
    }

    @Override
    protected void onEnable() {
        InputManager im = app.getInputManager();
        im.addMapping(M_HELP, new KeyTrigger(KeyInput.KEY_F1));
        im.addMapping(M_BOOST, new KeyTrigger(KeyInput.KEY_SPACE));
        im.addMapping(M_RESET, new KeyTrigger(KeyInput.KEY_C));
        im.addMapping(M_DRAW, new KeyTrigger(KeyInput.KEY_Z), new KeyTrigger(KeyInput.KEY_Y));
        im.addMapping(M_END, new KeyTrigger(KeyInput.KEY_E));
        im.addMapping(M_ZOOM_IN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        im.addMapping(M_ZOOM_OUT, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        im.addMapping(M_ORBIT, new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));
        im.addMapping(M_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        im.addMapping(M_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        im.addMapping(M_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        im.addMapping(M_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        im.addMapping(M_KEY_LEFT, new KeyTrigger(KeyInput.KEY_LEFT));
        im.addMapping(M_KEY_RIGHT, new KeyTrigger(KeyInput.KEY_RIGHT));
        im.addMapping(M_KEY_UP, new KeyTrigger(KeyInput.KEY_UP));
        im.addMapping(M_KEY_DOWN, new KeyTrigger(KeyInput.KEY_DOWN));
        im.addListener(input, MAPPINGS);
    }

    @Override
    protected void onDisable() {
        InputManager im = app.getInputManager();
        for (String m : MAPPINGS) {
            if (im.hasMapping(m)) im.deleteMapping(m);
        }
        im.removeListener(input);
        stage.animator().setBoost(1f);
    }

    @Override
    public void update(float tpf) {
        if (stage == null) return;
        stage.board().update(tpf);
        stage.hand().update(tpf);
        stage.hud().update(tpf, stage.board());
        stage.prompts().update();
        stage.dicePanel().layout();
        stage.rig().setShakeEnabled(app.getSettings().isCameraShake());
        boolean blocked = stage.prompts().isOpen() || (menu != null && menu.isOpen()) || inspector.isOpen();
        stage.hand().setInteractive(!blocked);
        if (pilot != null) pilot.update(tpf);
    }

    // ------------------------------------------------------------------
    // Zustand der Oberfläche
    // ------------------------------------------------------------------

    /**
     * Aktualisiert Oberfläche, spielbare Karten und Hervorhebungen.
     */
    public void refresh() {
        if (stage == null) return;
        ClientGameState model = logic.getModel();
        stage.hud().refresh(model, logic);
        stage.hand().setPlayable(logic::canPlay);
        boolean idle = !stage.animator().isBusy();
        for (SeatView seat : stage.board().seats()) {
            for (CardNode card : seat.visibleCards()) {
                Place place = stage.board().placeOf(card);
                if (place == null || card.getCard() == null) continue;
                ColorRGBA glow = null;
                if (card.getCard() instanceof Hero hero && place.kind() == BoardView.Kind.HERO) {
                    if (selectable.contains(hero)) glow = new ColorRGBA(1f, 0.25f, 0.2f, 1f);
                    else if (seat.getSeat() == 0 && idle && logic.canActivate(hero)) glow = Theme.withAlpha(Theme.GOLD, 0.9f);
                    card.setBrightness(seat.getSeat() == 0 && model.isMyTurn() && model.isHeroUsed(hero) ? 0.62f : 1f);
                }
                if (place.kind() != BoardView.Kind.HOVER) card.setHighlight(glow, true);
            }
        }
        for (int i = 0; i < 3; i++) {
            CardNode monster = stage.board().openMonster(i);
            if (monster == null || !(monster.getCard() instanceof Monster m)) continue;
            monster.setHighlight(idle && logic.canAttack(m) ? new ColorRGBA(1f, 0.35f, 0.2f, 0.95f) : null, true);
        }
        updateHint(model);
    }

    private void updateHint(ClientGameState model) {
        if (!app.getSettings().isShowTips() || finished) {
            stage.hud().setHint("");
            return;
        }
        String hint = "";
        if (!selectable.isEmpty()) {
            hint = "Wähle einen rot leuchtenden Helden.";
        }
        else if (logic.canAct()) {
            int ap = model.getActionPoints();
            if (ap <= 0) hint = "Keine Aktionspunkte mehr – beende deinen Zug.";
            else if (canAttackAny()) hint = "Tipp: Klicke ein leuchtendes Monster an, um es anzugreifen (2 AP).";
            else if (model.getHand().stream().anyMatch(logic::canPlay))
                hint = "Tipp: Klicke eine leuchtende Handkarte an, um sie auszuspielen.";
            else if (logic.canDraw()) hint = "Tipp: Ziehe eine Karte (Z) oder beende deinen Zug (E).";
        }
        stage.hud().setHint(hint);
    }

    private boolean canAttackAny() {
        Monster[] open = logic.getModel().getOpenMonsters();
        if (open == null) return false;
        for (Monster m : open) if (m != null && logic.canAttack(m)) return true;
        return false;
    }

    private void setHeroSelection(List<Hero> candidates) {
        selectable.clear();
        if (candidates != null) selectable.addAll(candidates);
        refresh();
    }

    // ------------------------------------------------------------------
    // Aktionen der Oberfläche
    // ------------------------------------------------------------------

    @Override
    public void draw() {
        if (logic.canDraw()) logic.drawCard();
        else reject("Du kannst gerade keine Karte ziehen.");
    }

    @Override
    public void mulligan() {
        if (logic.canMulligan()) logic.mulligan();
        else reject("Eine neue Hand kostet " + Action.MULLIGAN.getCost() + " Aktionspunkte.");
    }

    @Override
    public void endTurn() {
        if (logic.canAct()) logic.endTurn();
    }

    @Override
    public void openMenu() {
        if (menu != null && menu.isOpen()) {
            menu.close();
            return;
        }
        menu = new Dialog(app.getUi(), app, "Pause", "Das Spiel läuft im Hintergrund weiter.")
                .addButton("Weiterspielen", ButtonVariant.SUCCESS, null)
                .addButton("Einstellungen", ButtonVariant.SECONDARY, () -> app.showSettings(true))
                .addButton("Regeln", ButtonVariant.SECONDARY, () -> app.showHelp(true))
                .addButton("Spiel verlassen", ButtonVariant.PRIMARY, this::confirmLeave);
        menu.show();
    }

    /**
     * Esc: schließt die Großansicht oder das Pausenmenü bzw. öffnet es.
     */
    public void onEscape() {
        if (inspector != null && inspector.isOpen()) inspector.close();
        else openMenu();
    }

    private void confirmLeave() {
        String text = switch (session.mode()) {
            case JOIN -> "Ein Bot übernimmt deinen Platz.";
            case HOST -> "Als Gastgeber beendest du damit das Spiel für alle.";
            case SOLO -> "Die laufende Partie gegen die Bots wird beendet.";
        };
        new Dialog(app.getUi(), app, "Spiel verlassen?", text)
                .addButton("Bleiben", ButtonVariant.SECONDARY, null)
                .addButton("Verlassen", ButtonVariant.PRIMARY, this::leave)
                .show();
    }

    private void leave() {
        app.leaveSession();
        app.showMainMenu();
    }

    @Override
    public void openHelp() {
        app.showHelp(true);
    }

    private void reject(String text) {
        stage.hud().toast(text, true);
    }

    @Override
    public void play(Card card) {
        if (logic.canPlay(card)) {
            logic.playCard(card);
            return;
        }
        stage.hand().shake(card);
        if (!(card instanceof Hero) && !(card instanceof Spell)) reject(Texts.reactionOnly(card));
        else if (!logic.canAct()) reject("Du bist gerade nicht am Zug.");
        else if (card instanceof Hero && logic.getModel().getMe().map(p -> p.getHeroCount() >= Group.SIZE).orElse(false))
            reject("Deine Gruppe ist voll.");
        else reject("Nicht genug Aktionspunkte.");
    }

    @Override
    public void inspect(Card card) {
        stage.sounds().play(Sfx.CARD_FLIP, 0.6f, 0.1f);
        inspector.show(card);
    }

    @Override
    public void cardClicked(CardNode card, Place place, int button) {
        if (Boolean.getBoolean("winf.uidump.input"))
            System.getLogger("pp.winf2slay.ui").log(System.Logger.Level.WARNING, "KARTE " + place + " "
                    + (card == null || card.getCard() == null ? "-" : card.getCard().getName()) + " Taste " + button);
        if (stage == null || finished) return;
        if (place.kind() == BoardView.Kind.DECK) {
            if (button == 0) draw();
            return;
        }
        if (card == null || card.getCard() == null) return;
        if (button == 1) {
            inspect(card.getCard());
            return;
        }
        Card c = card.getCard();
        if (c instanceof Hero hero && selectable.contains(hero)) {
            stage.prompts().close();
            logic.chooseHero(hero);
            setHeroSelection(null);
            return;
        }
        if (place.kind() == BoardView.Kind.HERO && place.seat() == 0 && c instanceof Hero hero) {
            if (logic.canActivate(hero)) logic.activateHero(hero);
            else if (!logic.canAct()) reject("Du bist gerade nicht am Zug.");
            else if (logic.getModel().isHeroUsed(hero)) reject("Dieser Held wurde in diesem Zug schon genutzt.");
            else reject("Nicht genug Aktionspunkte.");
        }
        else if (place.kind() == BoardView.Kind.OPEN_MONSTER && c instanceof Monster monster) {
            if (logic.canAttack(monster)) logic.attackMonster(monster);
            else if (!logic.canAct()) reject("Du bist gerade nicht am Zug.");
            else if (logic.getModel().getMe().map(p -> p.getHeroCount() < Action.HEROES_TO_ATTACK).orElse(true))
                reject("Für einen Angriff brauchst du mindestens " + Action.HEROES_TO_ATTACK + " Helden.");
            else reject("Ein Angriff kostet " + Action.ATTACK_MONSTER.getCost() + " Aktionspunkte.");
        }
    }

    @Override
    public void cardHovered(CardNode card, Place place, boolean entered) {
        if (stage == null) return;
        if (entered) {
            if (hovered != null && hovered != card) hovered.setHovered(false);
            hovered = card;
            boolean lift = place != null && (place.kind() == BoardView.Kind.HERO || place.kind() == BoardView.Kind.OPEN_MONSTER
                                             || place.kind() == BoardView.Kind.LEADER || place.kind() == BoardView.Kind.TROPHY);
            card.setHovered(lift);
            if (card.getCard() != null) stage.hud().showPreview(card.getCard());
        }
        else if (hovered == card) {
            card.setHovered(false);
            hovered = null;
            stage.hud().showPreview(null);
        }
    }

    // ------------------------------------------------------------------
    // Spielende und Verbindungsabbruch
    // ------------------------------------------------------------------

    private void showGameOver(GameOverEvent e) {
        finished = true;
        stage.hud().setHint("");
        stage.hud().showPreview(null);
        stage.hud().showFinalBanner(e.won() ? "Du hast gewonnen!" : "Spielende");
        Fx fx = stage.fx();
        if (e.won()) {
            stage.sounds().play(Sfx.WIN);
            stage.animator().play(fx.confetti(7f));
            stage.animator().play(Tweens.seq(Tweens.delay(0.2f), stage.hud().stamp("Gewonnen!", Theme.GOLD, 150)));
        }
        else {
            stage.sounds().play(Sfx.LOSE);
            stage.animator().play(stage.hud().stamp(e.winner() == null ? "Spielende" : e.winner() + " gewinnt",
                                                    Theme.RED, 110));
        }
        String title = e.won() ? "Du hast gewonnen!" : e.winner() == null ? "Spiel beendet"
                                                                          : e.winner() + " hat gewonnen";
        String reason = e.reason() == null ? "" : e.reason();
        stage.animator().play(Tweens.seq(Tweens.delay(e.won() ? 3.4f : 2.2f), Tweens.call(() -> {
            Dialog d = new Dialog(app.getUi(), app, title, reason).dismissable(false);
            if (session.mode() == Session.Mode.SOLO)
                d.addButton("Nochmal spielen", ButtonVariant.SUCCESS, () -> app.restartSolo());
            d.addButton("Zum Hauptmenü", ButtonVariant.PRIMARY, this::leave);
            d.show();
        })));
    }

    @Override
    public void onDisconnected(DisconnectedEvent event) {
        if (finished) return;
        finished = true;
        app.showError("Verbindung getrennt", event.reason());
    }

    /**
     * Tastatur und Maus für Kamera und Abkürzungen.
     */
    private final class InputHandler implements ActionListener, AnalogListener {
        @Override
        public void onAction(String name, boolean pressed, float tpf) {
            switch (name) {
                case M_BOOST -> stage.animator().setBoost(pressed ? 4f : 1f);
                case M_ORBIT -> orbiting = pressed;
                default -> {
                    if (!pressed) return;
                    // Abkürzungen nicht „durch“ Dialoge, Einstellungen oder Regeln hindurch
                    if (Dialog.anyOpen() || app.hasOverlay() || inspector.isOpen()) return;
                    switch (name) {
                        case M_HELP -> openHelp();
                        case M_RESET -> stage.rig().reset();
                        case M_DRAW -> draw();
                        case M_END -> endTurn();
                        default -> {
                            // nicht belegt
                        }
                    }
                }
            }
        }

        @Override
        public void onAnalog(String name, float value, float tpf) {
            switch (name) {
                case M_ZOOM_IN -> stage.rig().zoom(value * 0.5f);
                case M_ZOOM_OUT -> stage.rig().zoom(-value * 0.5f);
                case M_LEFT -> {
                    if (orbiting) stage.rig().orbit(-value * 120, 0);
                }
                case M_RIGHT -> {
                    if (orbiting) stage.rig().orbit(value * 120, 0);
                }
                case M_UP -> {
                    if (orbiting) stage.rig().orbit(0, value * 80);
                }
                case M_DOWN -> {
                    if (orbiting) stage.rig().orbit(0, -value * 80);
                }
                // Tasten liefern die Haltedauer: 60° bzw. 40° pro Sekunde
                case M_KEY_LEFT -> stage.rig().orbit(-value * 60, 0);
                case M_KEY_RIGHT -> stage.rig().orbit(value * 60, 0);
                case M_KEY_UP -> stage.rig().orbit(0, value * 40);
                case M_KEY_DOWN -> stage.rig().orbit(0, -value * 40);
                default -> {
                    // nicht belegt
                }
            }
        }
    }
}
