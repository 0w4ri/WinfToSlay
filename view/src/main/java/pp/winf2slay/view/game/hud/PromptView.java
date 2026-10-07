package pp.winf2slay.view.game.hud;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Button.ButtonAction;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.die.DiceResult;
import pp.winf2slay.view.anim.Animator;
import pp.winf2slay.view.audio.Sfx;
import pp.winf2slay.view.game.Texts;
import pp.winf2slay.view.game.card.CardFactory;
import pp.winf2slay.view.ui.ButtonVariant;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.Ui;
import pp.winf2slay.view.ui.UiAnimations;
import pp.winf2slay.view.ui.UiScale;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Anfragen des Servers an den Spieler: Herausforderung, Modifikationen und
 * Zielauswahl (Held oder Spieler).
 *
 * <p>Die Panels sind nicht modal – bei der Heldenauswahl kann direkt auf die
 * leuchtenden Karten am Tisch geklickt werden.</p>
 */
public class PromptView {

    private final Ui ui;
    private final Camera cam;
    private final CardFactory cards;
    private final com.jme3.app.Application app;
    private final Node root = new Node("prompt");
    private Container panel;
    private boolean top;
    private String title;
    private float lastWidth;
    private float lastHeight;

    /**
     * @param ui       Oberflächenfabrik
     * @param app      Anwendung
     * @param animator Animationen (für Einblendungen)
     * @param cards    Kartenfabrik
     * @param guiNode  Oberflächenknoten
     */
    public PromptView(Ui ui, com.jme3.app.Application app, Animator animator, CardFactory cards, Node guiNode) {
        this.ui = ui;
        this.app = app;
        this.cam = app.getCamera();
        this.cards = cards;
        guiNode.attachChild(root);
    }

    /**
     * @return {@code true}, solange eine Anfrage angezeigt wird
     */
    /**
     * @return Überschrift der offenen Anfrage oder {@code null}
     */
    public String getTitle() {
        return isOpen() ? title : null;
    }

    /**
     * @return {@code true}, solange eine Anfrage angezeigt wird
     */
    public boolean isOpen() {
        return panel != null;
    }

    /**
     * Schließt die aktuelle Anfrage.
     */
    public void close() {
        if (panel != null) panel.removeFromParent();
        panel = null;
    }

    /**
     * Entfernt die Anfrageebene.
     */
    public void detach() {
        close();
        root.removeFromParent();
    }

    private Container open(String heading, String icon, boolean atTop) {
        close();
        top = atTop;
        title = heading;
        panel = ui.panel();
        Container head = ui.row();
        head.addChild(ui.icon(icon, 44, Theme.GOLD));
        head.addChild(ui.spacer(10, 4));
        head.addChild(ui.heading(heading, 40, Theme.GOLD));
        panel.addChild(head);
        root.attachChild(panel);
        ui.sounds().play(Sfx.BANNER, 0.7f, 0f);
        return panel;
    }

    private void show() {
        layout();
        UiAnimations.popIn(app, panel, panel.getPreferredSize());
    }

    /**
     * Ordnet das Panel neu an (Fenstergröße).
     */
    public void layout() {
        lastWidth = cam.getWidth();
        lastHeight = cam.getHeight();
        if (panel == null) return;
        float s = UiScale.of(cam);
        root.setLocalScale(s);
        root.setLocalTranslation(0, 0, 50);
        Vector3f size = panel.getPreferredSize();
        float w = UiScale.width(cam);
        float h = UiScale.height(cam);
        float y = top ? h - 150 : (h + size.y) / 2 + 60;
        panel.setLocalTranslation((w - size.x) / 2, y, 0);
    }

    /**
     * Prüft pro Bild, ob sich die Fenstergröße geändert hat.
     */
    public void update() {
        if (cam.getWidth() != lastWidth || cam.getHeight() != lastHeight) layout();
    }

    private Label cardImage(Card card, float width) {
        Label l = new Label("", Theme.STYLE);
        IconComponent icon = new IconComponent(cards.front(card), new Vector2f(1, 1), 0, 0, 0.02f, false);
        icon.setIconSize(new Vector2f(width, width * 1.4f));
        l.setIcon(icon);
        return l;
    }

    @SuppressWarnings("unchecked")
    private Button cardButton(Card card, float width, Runnable action) {
        Button b = new Button("", Theme.STYLE);
        b.setName("button:card:" + card.getName());
        IconComponent icon = new IconComponent(cards.front(card), new Vector2f(1, 1), 6, 6, 0.02f, false);
        icon.setIconSize(new Vector2f(width, width * 1.4f));
        b.setIcon(icon);
        // fester Hintergrund, nur die Farbe wechselt (siehe Ui#installStates)
        QuadBackgroundComponent bg = ui.theme().solid(new ColorRGBA(0, 0, 0, 0));
        b.setBackground(bg);
        b.addCommands(ButtonAction.HighlightOn, src -> {
            bg.setColor(Theme.withAlpha(Theme.GOLD, 0.55f));
            ui.sounds().play(Sfx.HOVER);
        });
        b.addCommands(ButtonAction.HighlightOff, src -> bg.setColor(new ColorRGBA(0, 0, 0, 0)));
        b.addClickCommands(src -> {
            ui.sounds().play(Sfx.CLICK);
            action.run();
        });
        return b;
    }

    // ------------------------------------------------------------------
    // Anfragen
    // ------------------------------------------------------------------

    /**
     * Fragt, ob eine gespielte Karte herausgefordert werden soll.
     *
     * @param player  ausspielender Spieler
     * @param card    gespielte Karte
     * @param respond Antwort ({@code true} = herausfordern)
     */
    public void showChallenge(String player, Card card, Consumer<Boolean> respond) {
        Container p = open("Herausfordern?", "sports_martial_arts", false);
        Container row = ui.row();
        row.addChild(cardImage(card, 170));
        row.addChild(ui.spacer(20, 10));
        Container text = ui.column();
        text.addChild(ui.wrapped(player + " spielt " + card.getDisplayName() + ".", 28, Theme.CREAM, 420));
        text.addChild(ui.spacer(8, 10));
        text.addChild(ui.wrapped("Mit einer Herausforderungskarte kannst du das verhindern: Ihr würfelt beide, "
                                 + "wer höher würfelt, gewinnt.", 22, Theme.MUTED, 420));
        row.addChild(text);
        p.addChild(row);
        p.addChild(ui.spacer(10, 16));
        Container buttons = ui.row();
        buttons.addChild(ui.button("Herausfordern", "sports_martial_arts", ButtonVariant.PRIMARY, () -> {
            close();
            respond.accept(true);
        }));
        buttons.addChild(ui.spacer(16, 10));
        buttons.addChild(ui.button("Durchlassen", "check", ButtonVariant.SECONDARY, () -> {
            close();
            respond.accept(false);
        }));
        p.addChild(buttons);
        show();
    }

    /**
     * Bietet Modifikationskarten für einen einzelnen Wurf an.
     *
     * @param result     aktueller Wurf
     * @param purpose    Zweck
     * @param threshold  Zielwert
     * @param higherWins Richtung des Ziels
     * @param mods       eigene Modifikationskarten
     * @param respond    Antwort (Karte oder {@code null})
     */
    public void showModifySingle(DiceResult result, RollPurpose purpose, int threshold, boolean higherWins,
                                 List<Modification> mods, Consumer<Modification> respond) {
        Container p = open("Wurf verändern?", "casino", false);
        p.addChild(ui.text(result.getPlayerName() + " würfelt für: " + Texts.purpose(purpose), 26, Theme.CREAM));
        boolean success = higherWins ? result.getTotal() >= threshold : result.getTotal() <= threshold;
        Label score = ui.heading("Ergebnis " + result.getTotal() + "   ·   Ziel " + Texts.goal(threshold, higherWins),
                                 34, success ? Theme.GREEN : Theme.RED);
        p.addChild(score);
        p.addChild(ui.text(Texts.breakdown(result), 22, Theme.MUTED));
        p.addChild(ui.spacer(10, 10));
        p.addChild(modRow(mods, mod -> {
            close();
            respond.accept(mod);
        }));
        p.addChild(ui.spacer(10, 10));
        p.addChild(ui.button("Nicht verändern", "block", ButtonVariant.SECONDARY, () -> {
            close();
            respond.accept(null);
        }));
        show();
    }

    private Container modRow(List<Modification> mods, Consumer<Modification> choose) {
        Container row = ui.row();
        List<Modification> distinct = mods.stream().distinct().toList();
        for (Modification mod : distinct) {
            long count = mods.stream().filter(mod::equals).count();
            Container cell = ui.column();
            cell.addChild(cardButton(mod, 110, () -> choose.accept(mod)));
            Label label = ui.outlined(Texts.signed(mod.getDelta()) + (count > 1 ? "  (" + count + "×)" : ""), 24,
                                      mod.getDelta() > 0 ? Theme.GREEN : Theme.RED);
            label.setTextHAlignment(HAlignment.Center);
            cell.addChild(label);
            row.addChild(cell);
            row.addChild(ui.spacer(10, 10));
        }
        return row;
    }

    /**
     * Bietet Modifikationskarten während einer Herausforderung an.
     *
     * @param active     Wurf des ausspielenden Spielers
     * @param challenger Wurf des Herausforderers
     * @param mods       eigene Modifikationskarten
     * @param respond    Antwort (Karte und Zielspieler oder {@code null, null})
     */
    public void showModifyDouble(DiceResult active, DiceResult challenger, List<Modification> mods,
                                 BiConsumer<Modification, String> respond) {
        Container p = open("Duell beeinflussen?", "casino", false);
        Container scores = ui.row();
        scores.addChild(scoreBox(active));
        Label vs = ui.title("VS", 54);
        vs.setTextVAlignment(VAlignment.Center);
        scores.addChild(vs);
        scores.addChild(scoreBox(challenger));
        p.addChild(scores);
        Label instruction = ui.text("Wähle eine Karte und dann, wessen Wurf sie verändert:", 22, Theme.MUTED);
        p.addChild(instruction);
        Container targets = ui.row();
        p.addChild(modRow(mods, mod -> {
            targets.clearChildren();
            instruction.setText(Texts.signed(mod.getDelta()) + " anwenden auf …");
            for (DiceResult r : List.of(active, challenger)) {
                targets.addChild(ui.button(r.getPlayerName(), "person", ButtonVariant.GOLD, () -> {
                    close();
                    respond.accept(mod, r.getPlayerName());
                }));
                targets.addChild(ui.spacer(12, 10));
            }
        }));
        p.addChild(targets);
        p.addChild(ui.spacer(10, 8));
        p.addChild(ui.button("Nicht eingreifen", "block", ButtonVariant.SECONDARY, () -> {
            close();
            respond.accept(null, null);
        }));
        show();
    }

    private Container scoreBox(DiceResult r) {
        Container box = ui.glass();
        box.addChild(ui.spacer(228, 1));
        Label name = ui.outlined(r.getPlayerName(), 26, Theme.CREAM);
        name.setTextHAlignment(HAlignment.Center);
        box.addChild(name);
        Label total = ui.title(String.valueOf(r.getTotal()), 72);
        box.addChild(total);
        Label detail = ui.text(Texts.breakdown(r), 19, Theme.MUTED);
        detail.setTextHAlignment(HAlignment.Center);
        box.addChild(detail);
        return box;
    }

    /**
     * Fordert zur Auswahl eines Helden auf (Klick am Tisch oder in der Liste).
     *
     * @param candidates mögliche Helden
     * @param owner      liefert den Besitzer eines Helden
     * @param choose     Auswahl
     */
    public void showHeroChoice(List<Hero> candidates, Function<Hero, String> owner, Consumer<Hero> choose) {
        Container p = open("Ziel wählen", "flash_on", true);
        p.addChild(ui.text("Klicke auf einen leuchtenden Helden – er wird zerstört.", 24, Theme.CREAM));
        Container row = ui.row();
        int shown = 0;
        for (Hero hero : candidates) {
            if (shown++ >= 8) break;
            Container cell = ui.column();
            cell.addChild(cardButton(hero, 78, () -> {
                close();
                choose.accept(hero);
            }));
            Label label = ui.text(owner.apply(hero), 17, Theme.MUTED);
            label.setTextHAlignment(HAlignment.Center);
            cell.addChild(label);
            row.addChild(cell);
            row.addChild(ui.spacer(6, 6));
        }
        p.addChild(row);
        show();
    }

    /**
     * Fordert zur Auswahl eines Spielers auf.
     *
     * @param candidates Spieler
     * @param info       Zusatzinfo je Spieler (z. B. Anzahl der Helden)
     * @param choose     Auswahl
     */
    public void showPlayerChoice(List<String> candidates, Function<String, String> info, Consumer<String> choose) {
        Container p = open("Spieler wählen", "group", false);
        p.addChild(ui.text("Alle Helden des gewählten Spielers werden zerstört.", 24, Theme.CREAM));
        p.addChild(ui.spacer(8, 10));
        for (String name : candidates) {
            Button b = ui.button(name + "   ·   " + info.apply(name), "person", ButtonVariant.GOLD, () -> {
                close();
                choose.accept(name);
            });
            b.setPreferredSize(new Vector3f(520, 64, 0));
            p.addChild(b);
            p.addChild(ui.spacer(6, 6));
        }
        show();
    }
}
