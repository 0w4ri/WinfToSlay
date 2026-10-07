package pp.winf2slay.view.game.hud;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.VAlignment;
import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.model.die.DiceResult;
import pp.winf2slay.view.anim.Easing;
import pp.winf2slay.view.anim.Tween;
import pp.winf2slay.view.anim.Tweens;
import pp.winf2slay.view.game.Texts;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.Ui;
import pp.winf2slay.view.ui.UiScale;

/**
 * Anzeige eines Würfelwurfs oben in der Bildmitte: Augen, Bonus, Summe und Ziel –
 * bei Herausforderungen beide Seiten im Vergleich.
 */
public class DicePanel {

    private final Ui ui;
    private final Camera cam;
    private final Node root = new Node("dice-panel");
    private Container panel;
    private Side left;
    private Side right;
    private Label goal;
    private int threshold;
    private boolean higherWins;

    /**
     * @param ui      Oberflächenfabrik
     * @param cam     Kamera
     * @param guiNode Oberflächenknoten
     */
    public DicePanel(Ui ui, Camera cam, Node guiNode) {
        this.ui = ui;
        this.cam = cam;
        guiNode.attachChild(root);
    }

    /**
     * Eine Seite der Anzeige (Spieler und Ergebnis).
     */
    private final class Side {
        private final Label name;
        private final Label dice;
        private final Label total;
        private final Container box;

        private Side(DiceResult result, String subtitle, ColorRGBA color) {
            box = ui.column();
            name = ui.outlined(result.getPlayerName(), 26, color);
            name.setTextHAlignment(HAlignment.Center);
            box.addChild(name);
            if (subtitle != null) {
                Label sub = ui.text(subtitle, 20, Theme.MUTED);
                sub.setTextHAlignment(HAlignment.Center);
                box.addChild(sub);
            }
            total = ui.title("?", 78);
            total.setTextHAlignment(HAlignment.Center);
            box.addChild(total);
            dice = ui.text("", 21, Theme.CREAM);
            dice.setTextHAlignment(HAlignment.Center);
            box.addChild(dice);
        }

        private void set(DiceResult r) {
            total.setText(String.valueOf(r.getTotal()));
            dice.setText(Texts.breakdown(r));
        }
    }

    private void open() {
        hideNow();
        panel = ui.glass();
        root.attachChild(panel);
    }

    private Tween slideIn() {
        Container p = panel;
        layout();
        Vector3f base = p.getLocalTranslation().clone();
        return Tweens.lerp(0.35f, Easing.OUT_BACK, t -> {
            p.setLocalTranslation(base.x, base.y + 120 * (1 - t), base.z);
            p.setAlpha(Math.min(1, t * 1.5f));
        });
    }

    /**
     * Ordnet das Panel neu an.
     */
    public void layout() {
        if (panel == null) return;
        float s = UiScale.of(cam);
        root.setLocalScale(s);
        root.setLocalTranslation(0, 0, 40);
        Vector3f size = panel.getPreferredSize();
        // links neben der Tischmitte – die Würfel selbst rollen in der Mitte des Tisches
        panel.setLocalTranslation(Math.max(390, UiScale.width(cam) / 2 - 560), UiScale.height(cam) - 450, 0);
    }

    /**
     * Zeigt einen einzelnen Wurf an (Werte erscheinen nach dem Rollen).
     *
     * @param result     Wurf
     * @param purpose    Zweck
     * @param threshold  Zielwert
     * @param higherWins Richtung
     * @param color      Spielerfarbe
     * @return Tween zum Einblenden
     */
    public Tween showSingle(DiceResult result, RollPurpose purpose, int threshold, boolean higherWins,
                            ColorRGBA color) {
        return Tweens.defer(() -> {
            open();
            this.threshold = threshold;
            this.higherWins = higherWins;
            left = new Side(result, Texts.purpose(purpose), color);
            right = null;
            Container row = ui.row();
            row.addChild(left.box);
            row.addChild(ui.spacer(26, 10));
            Container goalBox = ui.column();
            Label goalTitle = ui.text("Ziel", 20, Theme.MUTED);
            goalTitle.setTextHAlignment(HAlignment.Center);
            goalBox.addChild(goalTitle);
            goal = ui.heading(Texts.goal(threshold, higherWins), 34, Theme.GOLD);
            goal.setTextVAlignment(VAlignment.Center);
            goalBox.addChild(goal);
            row.addChild(goalBox);
            panel.addChild(row);
            return slideIn();
        });
    }

    /**
     * Enthüllt die Augenzahl (zählt hoch).
     *
     * @param result Wurf
     * @return Tween
     */
    public Tween reveal(DiceResult result) {
        return Tweens.defer(() -> countUp(left, result));
    }

    private Tween countUp(Side side, DiceResult result) {
        if (side == null) return Tweens.NONE;
        int target = result.getTotal();
        return Tweens.seq(Tweens.lerp(0.4f, Easing.OUT_QUAD, t -> {
            side.total.setText(String.valueOf(Math.round(target * t)));
            colorize(side, result);
        }), Tweens.call(() -> side.set(result)));
    }

    private void colorize(Side side, DiceResult result) {
        if (side == left && right == null) {
            boolean ok = higherWins ? result.getTotal() >= threshold : result.getTotal() <= threshold;
            side.total.setColor(ok ? Theme.GREEN.clone() : Theme.CREAM.clone());
        }
    }

    /**
     * Aktualisiert einen einzelnen Wurf nach einer Modifikation.
     *
     * @param result neuer Wurf
     * @return Tween
     */
    public Tween modified(DiceResult result) {
        return Tweens.defer(() -> bump(left, result));
    }

    private Tween bump(Side side, DiceResult result) {
        if (side == null) return Tweens.NONE;
        side.set(result);
        colorize(side, result);
        Label total = side.total;
        return Tweens.lerp(0.35f, Easing.OUT_ELASTIC, t -> total.setFontSize(78 * (1.35f - 0.35f * t)));
    }

    /**
     * Zeigt das Duell einer Herausforderung an.
     *
     * @param active     Wurf des ausspielenden Spielers
     * @param challenger Wurf des Herausforderers
     * @param ca         Farbe des ausspielenden Spielers
     * @param cb         Farbe des Herausforderers
     * @return Tween
     */
    public Tween showDouble(DiceResult active, DiceResult challenger, ColorRGBA ca, ColorRGBA cb) {
        return Tweens.defer(() -> {
            open();
            left = new Side(active, "spielt die Karte", ca);
            right = new Side(challenger, "fordert heraus", cb);
            Container row = ui.row();
            row.addChild(left.box);
            Label vs = ui.title("VS", 56);
            vs.setTextVAlignment(VAlignment.Center);
            vs.setInsets(new Insets3f(0, 30, 0, 30));
            row.addChild(vs);
            row.addChild(right.box);
            panel.addChild(row);
            return slideIn();
        });
    }

    /**
     * Enthüllt beide Würfe einer Herausforderung.
     *
     * @param active     Wurf des ausspielenden Spielers
     * @param challenger Wurf des Herausforderers
     * @return Tween
     */
    public Tween revealDouble(DiceResult active, DiceResult challenger) {
        return Tweens.defer(() -> Tweens.par(countUp(left, active), countUp(right, challenger)));
    }

    /**
     * Aktualisiert beide Würfe nach einer Modifikation.
     *
     * @param active     neuer Wurf des ausspielenden Spielers
     * @param challenger neuer Wurf des Herausforderers
     * @param target     veränderte Seite
     * @return Tween
     */
    public Tween modifiedDouble(DiceResult active, DiceResult challenger, String target) {
        return Tweens.defer(() -> {
            if (left == null || right == null) return Tweens.NONE;
            boolean leftChanged = active.getPlayerName().equals(target);
            if (leftChanged) {
                right.set(challenger);
                return bump(left, active);
            }
            left.set(active);
            return bump(right, challenger);
        });
    }

    /**
     * Färbt das Ergebnis nach der Auflösung ein.
     *
     * @param leftWins {@code true}, wenn die linke Seite gewonnen hat bzw. der Wurf gelungen ist
     * @return Tween
     */
    public Tween resolve(boolean leftWins) {
        return Tweens.call(() -> {
            if (left != null) left.total.setColor((leftWins ? Theme.GREEN : Theme.RED).clone());
            if (right != null) right.total.setColor((leftWins ? Theme.RED : Theme.GREEN).clone());
        });
    }

    /**
     * Blendet die Anzeige aus.
     *
     * @return Tween
     */
    public Tween hide() {
        return Tweens.defer(() -> {
            Container p = panel;
            if (p == null) return Tweens.NONE;
            panel = null;
            left = null;
            right = null;
            return Tweens.seq(Tweens.lerp(0.25f, Easing.IN_QUAD, t -> p.setAlpha(1 - t)),
                              Tweens.call(p::removeFromParent));
        });
    }

    /**
     * Entfernt die Anzeige sofort.
     */
    public void hideNow() {
        if (panel != null) panel.removeFromParent();
        panel = null;
    }

    /**
     * Entfernt die Ebene.
     */
    public void detach() {
        hideNow();
        root.removeFromParent();
    }
}
