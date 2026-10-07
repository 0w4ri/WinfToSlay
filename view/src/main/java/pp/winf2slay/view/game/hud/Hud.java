package pp.winf2slay.view.game.hud;

import com.jme3.app.Application;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import pp.winf2slay.controller.client.ClientGameLogic;
import pp.winf2slay.controller.client.ClientGameState;
import pp.winf2slay.controller.message.server.PlayerSnapshot;
import pp.winf2slay.model.Action;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.view.anim.Animator;
import pp.winf2slay.view.anim.Easing;
import pp.winf2slay.view.anim.Tween;
import pp.winf2slay.view.anim.Tweens;
import pp.winf2slay.view.audio.Sfx;
import pp.winf2slay.view.game.Texts;
import pp.winf2slay.view.game.board.BoardView;
import pp.winf2slay.view.game.board.SeatView;
import pp.winf2slay.view.game.card.CardFactory;
import pp.winf2slay.view.game.card.CardSprite;
import pp.winf2slay.view.ui.ButtonVariant;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.Ui;
import pp.winf2slay.view.ui.UiScale;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Die Spieloberfläche über dem Tisch: Zuganzeige mit Aktionspunkten,
 * Spielerübersicht, Protokoll, Aktionsknöpfe, Hinweise und Kartenvorschau.
 */
public class Hud {

    /**
     * Aktionen, die über die Oberfläche ausgelöst werden.
     */
    public interface Actions {
        /** Karte ziehen. */
        void draw();

        /** Neue Hand (Mulligan). */
        void mulligan();

        /** Zug beenden. */
        void endTurn();

        /** Spielmenü öffnen. */
        void openMenu();

        /** Spielregeln öffnen. */
        void openHelp();
    }

    private static final int LOG_LINES = 6;
    private static final float TOAST_SECONDS = 3.2f;

    private final Ui ui;
    private final Camera cam;
    private final Animator animator;
    private final CardFactory cards;
    private final Actions actions;
    private final Node root = new Node("hud");
    private final Map<String, PlayerPanel> panels = new LinkedHashMap<>();
    private final Map<String, Label> seatLabels = new LinkedHashMap<>();
    private final List<Label> logLines = new ArrayList<>();
    private final List<Toast> toasts = new ArrayList<>();

    private Container banner;
    private Label bannerText;
    private String finalBanner;
    private Container apRow;
    private final List<Label> apGems = new ArrayList<>();
    private Container playersBox;
    private Container logBox;
    private Container buttons;
    private Button drawButton;
    private Button mulliganButton;
    private Button endButton;
    private Container menuButtons;
    private Label hint;
    private Node preview;
    private Card previewCard;
    private float lastWidth;
    private float lastHeight;
    private int shownAp = -1;

    /**
     * @param ui      Oberflächenfabrik
     * @param app     Anwendung
     * @param animator Animationen
     * @param cards   Kartenfabrik (für die Vorschau)
     * @param actions Empfänger der Knöpfe
     */
    public Hud(Ui ui, Application app, Animator animator, CardFactory cards, Actions actions) {
        this.ui = ui;
        this.cam = app.getCamera();
        this.animator = animator;
        this.cards = cards;
        this.actions = actions;
    }

    /**
     * Baut die Oberfläche auf.
     *
     * @param guiNode Oberflächenknoten
     * @param seating Spieler (eigener Platz zuerst)
     */
    public void build(Node guiNode, List<String> seating) {
        guiNode.attachChild(root);
        root.setLocalTranslation(0, 0, 10);

        banner = new Container(Theme.STYLE);
        banner.setBackground(new QuadBackgroundComponent(ui.theme().texture("ui/ribbon.png")));
        banner.setPreferredSize(new Vector3f(640, 125, 0));
        bannerText = ui.heading("", 40, Theme.CREAM);
        bannerText.setTextHAlignment(HAlignment.Center);
        bannerText.setTextVAlignment(VAlignment.Center);
        bannerText.setInsets(new Insets3f(14, 60, 40, 60));
        banner.addChild(bannerText);
        root.attachChild(banner);

        apRow = ui.row();
        Label apLabel = ui.outlined("Aktionspunkte", 22, Theme.CREAM);
        apRow.addChild(apLabel);
        apRow.addChild(ui.spacer(8, 4));
        for (int i = 0; i < Action.POINTS_PER_TURN; i++) {
            Label gem = new Label("", Theme.STYLE);
            gem.setIcon(gemIcon(false));
            gem.setPreferredSize(new Vector3f(42, 42, 0));
            apGems.add(gem);
            apRow.addChild(gem);
        }
        root.attachChild(apRow);

        playersBox = ui.column();
        for (int i = 0; i < seating.size(); i++) {
            String player = seating.get(i);
            PlayerPanel panel = new PlayerPanel(ui, player, Theme.playerColor(i), i == 0);
            panels.put(player, panel);
            playersBox.addChild(panel);
            playersBox.addChild(ui.spacer(4, 6));
            if (i > 0) {
                Label label = ui.outlined(player, 21, Theme.playerColor(i));
                label.setTextHAlignment(HAlignment.Center);
                seatLabels.put(player, label);
                root.attachChild(label);
            }
        }
        root.attachChild(playersBox);

        logBox = ui.glass();
        logBox.addChild(ui.spacer(350, 1));
        Label logTitle = ui.heading("Spielverlauf", 26, Theme.GOLD);
        logBox.addChild(logTitle);
        for (int i = 0; i < LOG_LINES; i++) {
            Label line = ui.wrapped("", 18, Theme.CREAM, 350);
            logLines.add(line);
            logBox.addChild(line);
        }
        root.attachChild(logBox);

        buttons = ui.column();
        drawButton = ui.button("Karte ziehen (1 AP)", "layers", ButtonVariant.SECONDARY, actions::draw);
        mulliganButton = ui.button("Neue Hand (3 AP)", "autorenew", ButtonVariant.SECONDARY, actions::mulligan);
        endButton = ui.button("Zug beenden", "skip_next", ButtonVariant.PRIMARY, actions::endTurn);
        for (Button b : new Button[]{drawButton, mulliganButton, endButton}) {
            b.setFontSize(25);
            b.setPreferredSize(new Vector3f(420, 62, 0));
            buttons.addChild(b);
            buttons.addChild(ui.spacer(4, 8));
        }
        root.attachChild(buttons);

        menuButtons = ui.row();
        menuButtons.addChild(ui.roundButton("settings", 64, actions::openMenu));
        menuButtons.addChild(ui.spacer(8, 8));
        menuButtons.addChild(ui.roundButton("help_outline", 64, actions::openHelp));
        root.attachChild(menuButtons);

        hint = ui.outlined("", 24, Theme.CREAM);
        hint.setTextHAlignment(HAlignment.Center);
        root.attachChild(hint);

        layout();
    }

    private com.simsilica.lemur.component.IconComponent gemIcon(boolean full) {
        com.simsilica.lemur.component.IconComponent icon = new com.simsilica.lemur.component.IconComponent(
                ui.theme().texture(full ? "ui/ap_full.png" : "ui/ap_empty.png"), new com.jme3.math.Vector2f(1, 1),
                0, 0, 0.02f, false);
        icon.setIconSize(new com.jme3.math.Vector2f(40, 40));
        icon.setHAlignment(HAlignment.Center);
        icon.setVAlignment(VAlignment.Center);
        return icon;
    }

    /**
     * Ordnet alle Elemente für die aktuelle Fenstergröße an.
     */
    public void layout() {
        float s = UiScale.of(cam);
        float w = UiScale.width(cam);
        float h = UiScale.height(cam);
        lastWidth = cam.getWidth();
        lastHeight = cam.getHeight();
        root.setLocalScale(s);
        root.setLocalTranslation(0, 0, 10);
        Vector3f bs = banner.getPreferredSize();
        banner.setLocalTranslation((w - bs.x) / 2, h - 6, 0);
        Vector3f ap = apRow.getPreferredSize();
        apRow.setLocalTranslation((w - ap.x) / 2, h - bs.y + 6, 0);
        menuButtons.setLocalTranslation(22, h - 20, 0);
        playersBox.setLocalTranslation(22, h - 104, 0);
        Vector3f lb = logBox.getPreferredSize();
        logBox.setLocalTranslation(w - lb.x - 22, h - 22, 0);
        Vector3f bb = buttons.getPreferredSize();
        buttons.setLocalTranslation(w - bb.x - 26, bb.y + 18, 0);
        Vector3f hs = hint.getPreferredSize();
        hint.setLocalTranslation((w - hs.x) / 2, 360, 0);
        layoutToasts();
    }

    /**
     * Entfernt die Oberfläche.
     */
    public void detach() {
        root.removeFromParent();
    }

    /**
     * @param visible Sichtbarkeit
     */
    public void setVisible(boolean visible) {
        root.setCullHint(visible ? Node.CullHint.Inherit : Node.CullHint.Always);
    }

    // ------------------------------------------------------------------
    // Zustand
    // ------------------------------------------------------------------

    /**
     * Übernimmt den Spielzustand.
     *
     * @param model Spielzustand
     * @param logic Client-Logik (für erlaubte Aktionen)
     */
    public void refresh(ClientGameState model, ClientGameLogic logic) {
        for (PlayerSnapshot p : model.getPlayers()) {
            PlayerPanel panel = panels.get(p.getName());
            if (panel != null) {
                panel.update(p);
                panel.setActive(p.getName().equals(model.getActivePlayer()));
            }
            Label seat = seatLabels.get(p.getName());
            if (seat != null) {
                seat.setText(p.getName() + (p.isBot() ? " (Bot)" : "") + "  ·  " + p.getHandCount() + " Karten");
            }
        }
        boolean mine = model.isMyTurn();
        drawButton.setEnabled(logic.canDraw());
        mulliganButton.setEnabled(logic.canMulligan());
        endButton.setEnabled(logic.canAct());
        apRow.setCullHint(mine ? Node.CullHint.Inherit : Node.CullHint.Always);
        int points = mine ? model.getActionPoints() : 0;
        if (points != shownAp) {
            for (int i = 0; i < apGems.size(); i++) {
                boolean full = i < points;
                apGems.get(i).setIcon(gemIcon(full));
                if (shownAp > points && i == points) popGem(apGems.get(i));
            }
            shownAp = points;
        }
        if (finalBanner != null) {
            setBannerText(finalBanner);
            apRow.setCullHint(Node.CullHint.Always);
            return;
        }
        setBannerText(mine ? "Du bist am Zug" : model.getActivePlayer() + " ist am Zug");
    }

    /**
     * Zeigt nach Spielende dauerhaft einen Abschlusstext im Banner und sperrt die Aktionen.
     *
     * @param text Text, z. B. „Du hast gewonnen!“
     */
    public void showFinalBanner(String text) {
        finalBanner = text;
        setBannerText(text);
        apRow.setCullHint(Node.CullHint.Always);
        drawButton.setEnabled(false);
        mulliganButton.setEnabled(false);
        endButton.setEnabled(false);
    }

    /**
     * Ein verbrauchter Aktionspunkt hüpft kurz auf.
     */
    private void popGem(Label gem) {
        ui.sounds().play(Sfx.AP, 1f, 0.05f);
        Vector3f base = gem.getLocalTranslation().clone();
        Vector3f size = gem.getSize().clone();
        animator.playUi(Tweens.lerp(0.35f, Easing.OUT_CUBIC, t -> {
            float k = 1 + 0.45f * FastMath.sin(t * FastMath.PI);
            gem.setLocalScale(k);
            gem.setLocalTranslation(base.x - size.x * (k - 1) / 2, base.y + size.y * (k - 1) / 2, base.z);
        }));
    }

    private void setBannerText(String text) {
        if (!text.equals(bannerText.getText())) bannerText.setText(text);
    }

    /**
     * Großes Banner beim Zugwechsel.
     *
     * @param player Spieler am Zug
     * @param mine   {@code true}, wenn es der eigene Zug ist
     * @return Tween
     */
    public Tween turnSplash(String player, boolean mine) {
        Container splash = new Container(Theme.STYLE);
        splash.setBackground(new QuadBackgroundComponent(ui.theme().texture("ui/ribbon.png")));
        splash.setPreferredSize(new Vector3f(1100, 215, 0));
        Label text = ui.title(mine ? "Dein Zug!" : player + " ist dran", mine ? 86 : 70);
        text.setTextVAlignment(VAlignment.Center);
        text.setInsets(new Insets3f(20, 90, 70, 90));
        splash.addChild(text);
        float w = UiScale.width(cam);
        float h = UiScale.height(cam);
        Vector3f size = splash.getPreferredSize();
        Node holder = new Node("splash");
        holder.attachChild(splash);
        splash.setLocalTranslation(-size.x / 2, size.y / 2, 0);
        return Tweens.seq(
                Tweens.call(() -> {
                    root.attachChild(holder);
                    holder.setLocalTranslation(w / 2, h * 0.58f, 60);
                    ui.sounds().play(mine ? Sfx.MY_TURN : Sfx.BANNER);
                }),
                Tweens.lerp(0.45f, Easing.OUT_BACK, t -> {
                    holder.setLocalScale(Math.max(0.01f, t), Math.max(0.01f, 0.3f + 0.7f * t), 1);
                    splash.setAlpha(Math.min(1, t * 2));
                }),
                Tweens.delay(mine ? 0.9f : 0.55f),
                Tweens.lerp(0.35f, Easing.IN_CUBIC, t -> {
                    holder.setLocalTranslation(w / 2, h * 0.58f + t * 80, 60);
                    splash.setAlpha(1 - t);
                }),
                Tweens.call(holder::removeFromParent));
    }

    /**
     * Zentrierter Schriftzug (z. B. „Erfolg!“), der kurz aufploppt.
     *
     * @param text  Text
     * @param color Farbe
     * @param size  Schriftgröße
     * @return Tween
     */
    public Tween stamp(String text, ColorRGBA color, float size) {
        Label label = ui.title(text, size);
        label.setColor(color.clone());
        Node holder = new Node("stamp");
        holder.attachChild(label);
        Vector3f ls = label.getPreferredSize();
        label.setLocalTranslation(-ls.x / 2, ls.y / 2, 0);
        float w = UiScale.width(cam);
        float h = UiScale.height(cam);
        return Tweens.seq(
                Tweens.call(() -> {
                    root.attachChild(holder);
                    holder.setLocalTranslation(w / 2, h * 0.52f, 65);
                }),
                Tweens.lerp(0.32f, Easing.OUT_BACK, t -> {
                    holder.setLocalScale(Math.max(0.01f, 2.2f - 1.2f * t));
                    label.setAlpha(Math.min(1, t * 2));
                }),
                Tweens.delay(0.7f),
                Tweens.lerp(0.3f, Easing.IN_QUAD, t -> label.setAlpha(1 - t)),
                Tweens.call(holder::removeFromParent));
    }

    // ------------------------------------------------------------------
    // Protokoll, Hinweise, Toasts
    // ------------------------------------------------------------------

    /**
     * Fügt eine Zeile zum Spielverlauf hinzu.
     *
     * @param text  Text
     * @param color Farbe
     */
    public void log(String text, ColorRGBA color) {
        for (int i = 0; i < LOG_LINES - 1; i++) {
            Label from = logLines.get(i + 1);
            Label to = logLines.get(i);
            to.setText(from.getText());
            to.setColor(from.getColor().clone());
        }
        Label last = logLines.getLast();
        last.setText(text);
        last.setColor(color.clone());
        for (int i = 0; i < LOG_LINES; i++) {
            ColorRGBA c = logLines.get(i).getColor().clone();
            c.a = 0.35f + 0.65f * (i + 1) / LOG_LINES;
            logLines.get(i).setColor(c);
        }
    }

    /**
     * Zeigt einen kurzen Hinweis unter dem Banner.
     *
     * @param text    Text
     * @param warning {@code true} für Warnungen (rot)
     */
    public void toast(String text, boolean warning) {
        Container box = new Container(Theme.STYLE);
        box.setBackground(ui.theme().toast());
        Container row = ui.row();
        row.addChild(ui.icon(warning ? "warning" : "info", 28, warning ? Theme.RED : Theme.GOLD));
        row.addChild(ui.spacer(8, 4));
        row.addChild(ui.text(text, 24, Theme.CREAM));
        box.addChild(row);
        root.attachChild(box);
        toasts.add(new Toast(box));
        if (toasts.size() > 3) toasts.removeFirst().panel.removeFromParent();
        if (warning) ui.sounds().play(Sfx.FAIL, 0.5f, 0f);
        layoutToasts();
    }

    private void layoutToasts() {
        float w = UiScale.width(cam);
        float y = UiScale.height(cam) - 190;
        for (Toast t : toasts) {
            Vector3f size = t.panel.getPreferredSize();
            t.panel.setLocalTranslation((w - size.x) / 2, y, 30);
            y -= size.y + 8;
        }
    }

    /**
     * @param text Hinweistext über der Hand (leer = ausblenden)
     */
    public void setHint(String text) {
        if (text.equals(hint.getText())) return;
        hint.setText(text);
        Vector3f hs = hint.getPreferredSize();
        hint.setLocalTranslation((UiScale.width(cam) - hs.x) / 2, 360, 0);
    }

    // ------------------------------------------------------------------
    // Kartenvorschau
    // ------------------------------------------------------------------

    /**
     * Zeigt eine Karte groß am rechten Rand.
     *
     * @param card Karte oder {@code null} zum Ausblenden
     */
    public void showPreview(Card card) {
        if (card == null ? previewCard == null : card.equals(previewCard)) return;
        if (preview != null) preview.removeFromParent();
        preview = null;
        previewCard = card;
        if (card == null) return;
        preview = new Node("preview");
        CardSprite sprite = new CardSprite(cards, card, 250);
        sprite.setLocalTranslation(0, 0, 1);
        preview.attachChild(sprite);
        Container info = ui.glass();
        info.addChild(ui.spacer(298, 1));
        info.addChild(ui.heading(card.getDisplayName(), 30, Theme.GOLD));
        info.addChild(ui.text(Texts.typeLine(card), 20, Theme.MUTED));
        info.addChild(ui.wrapped(Texts.describe(card), 19, Theme.CREAM, 290));
        Vector3f is = info.getPreferredSize();
        info.setLocalTranslation(-is.x / 2, -sprite.getHeight() / 2 - 12, 0);
        preview.attachChild(info);
        float w = UiScale.width(cam);
        preview.setLocalTranslation(w - 210, UiScale.height(cam) * 0.62f, 40);
        root.attachChild(preview);
        sprite.setAlpha(0);
        info.setAlpha(0);
        animator.playUi(Tweens.lerp(0.18f, t -> {
            sprite.setAlpha(t);
            info.setAlpha(t);
        }));
    }

    // ------------------------------------------------------------------
    // Laufende Aktualisierung
    // ------------------------------------------------------------------

    /**
     * Pro Bild: Namensschilder über den Plätzen, Toasts, Fenstergröße.
     *
     * @param tpf   Zeit seit dem letzten Bild
     * @param board Spieltisch
     */
    public void update(float tpf, BoardView board) {
        if (cam.getWidth() != lastWidth || cam.getHeight() != lastHeight) layout();
        float s = UiScale.of(cam);
        if (board != null) {
            for (SeatView seat : board.seats()) {
                Label label = seatLabels.get(seat.getPlayer());
                if (label == null) continue;
                Vector3f screen = cam.getScreenCoordinates(board.layout().seatCenter(seat.getSeat()));
                Vector3f size = label.getPreferredSize();
                float w = UiScale.width(cam);
                float h = UiScale.height(cam);
                float x = Math.max(380, Math.min(w - 470 - size.x, screen.x / s - size.x / 2));
                float y = screen.y / s + 95 + size.y / 2;
                // Ein Platz direkt gegenüber würde sein Schild unter das Banner schieben –
                // dann steht es stattdessen vor dem Platz (zur Tischmitte hin).
                boolean underBanner = Math.abs(x + size.x / 2 - w / 2) < 400 + size.x / 2;
                if (underBanner && y - size.y > h - 140) y = screen.y / s - 70;
                label.setLocalTranslation(x, y, 2);
                label.setAlpha(screen.z < 1 ? 1 : 0);
            }
        }
        for (Toast t : new ArrayList<>(toasts)) {
            t.age += tpf;
            if (t.age > TOAST_SECONDS) {
                float a = 1 - (t.age - TOAST_SECONDS) / 0.4f;
                t.panel.setAlpha(Math.max(0, a));
                if (a <= 0) {
                    t.panel.removeFromParent();
                    toasts.remove(t);
                    layoutToasts();
                }
            }
        }
    }

    /**
     * Ein Hinweis mit Alter.
     */
    private static final class Toast {
        private final Panel panel;
        private float age;

        private Toast(Panel panel) {
            this.panel = panel;
        }
    }
}
