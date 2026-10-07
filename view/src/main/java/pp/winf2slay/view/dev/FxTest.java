package pp.winf2slay.view.dev;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import pp.winf2slay.view.WinfToSlayApp;
import pp.winf2slay.view.anim.Tween;
import pp.winf2slay.view.fx.Fx;
import pp.winf2slay.view.ui.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Entwicklerwerkzeug ({@code -Dwinf.fxtest=true}): spielt alle Spezialeffekte
 * nacheinander in der Tischmitte ab.
 */
public class FxTest extends BaseAppState {

    private final List<Supplier<Tween>> effects = new ArrayList<>();
    private final Node overlay = new Node("fx-test-overlay");
    private Fx fx;
    private float wait = 2f;
    private int index;

    @Override
    protected void initialize(Application application) {
        WinfToSlayApp app = (WinfToSlayApp) application;
        app.getGuiNode().attachChild(overlay);
        fx = new Fx(app.getAssetManager(), app.getTable().getEffects(), overlay, app.getCamera(), app.getRig(),
                    app.getSounds(), app.getAnimator(), app.getUi().theme());
        app.getRig().gameView(true);
        Vector3f c = new Vector3f(0, 0, 10);
        effects.add(() -> fx.explosion(c, 1f));
        effects.add(() -> fx.lightning(c, new ColorRGBA(0.6f, 0.8f, 1f, 1f)));
        effects.add(() -> fx.mortar(c));
        effects.add(() -> fx.swords(c));
        effects.add(() -> fx.magicCircle(c, 16, Theme.BLUE, 1.3f));
        effects.add(() -> fx.beam(c, 26, Theme.GOLD, 1.1f));
        effects.add(() -> fx.slash(c, Theme.RED));
        effects.add(() -> fx.flash(c.add(0, 3, 0), 20, Theme.GOLD, 0.6f));
        effects.add(() -> fx.shockwave(c, 20, Theme.GOLD, 0.8f));
        effects.add(() -> fx.heliStrike(List.of(c, c.add(7, 0, 0), c.add(-7, 0, 0)), c));
        effects.add(() -> fx.bomb(new Vector3f(0, 0, 4)));
        effects.add(() -> fx.floatingText(c, "+4", Theme.GREEN, 80));
        effects.add(() -> fx.confetti(4f));
    }

    @Override
    protected void cleanup(Application application) {
        overlay.removeFromParent();
    }

    @Override
    protected void onEnable() {
        // nichts zu tun
    }

    @Override
    protected void onDisable() {
        // nichts zu tun
    }

    @Override
    public void update(float tpf) {
        WinfToSlayApp app = (WinfToSlayApp) getApplication();
        if (app.getAnimator().isBusy()) return;
        wait -= tpf;
        if (wait > 0 || index >= effects.size()) return;
        Tween t = effects.get(index++).get();
        app.getAnimator().enqueue(t);
        fx.sparks(new Vector3f(0, 1, 10), Theme.GOLD, 30, 12f);
        fx.twinkle(new Vector3f(-10, 1, 10), Theme.GOLD_LIGHT);
        fx.embers(new Vector3f(10, 1, 10));
        fx.smoke(new Vector3f(20, 1, 10), 4, true);
        wait = 1.5f;
    }
}
