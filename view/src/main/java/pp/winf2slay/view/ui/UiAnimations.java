package pp.winf2slay.view.ui;

import com.jme3.app.Application;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Panel;
import pp.winf2slay.view.anim.Animator;
import pp.winf2slay.view.anim.Easing;
import pp.winf2slay.view.anim.Tween;
import pp.winf2slay.view.anim.Tweens;

/**
 * Wiederkehrende Animationen für Oberflächenelemente.
 */
public final class UiAnimations {

    private UiAnimations() {
        // Utility-Klasse
    }

    private static Animator animator(Application app) {
        return app.getStateManager().getState(Animator.class);
    }

    /**
     * Lässt ein Panel aus der Mitte heraus aufploppen.
     *
     * @param app   Anwendung
     * @param panel Panel (Ursprung oben links)
     * @param size  Größe des Panels
     */
    public static void popIn(Application app, Panel panel, Vector3f size) {
        Animator animator = animator(app);
        if (animator == null) return;
        Vector3f base = panel.getLocalTranslation().clone();
        animator.playUi(Tweens.lerp(0.32f, Easing.OUT_BACK, t -> {
            float s = 0.82f + 0.18f * t;
            panel.setLocalScale(s);
            panel.setLocalTranslation(base.x + (1 - s) * size.x / 2, base.y - (1 - s) * size.y / 2, base.z);
            panel.setAlpha(Math.min(1, t * 1.6f));
        }));
    }

    /**
     * Schiebt ein Element von einem Versatz an seine Position und blendet es ein.
     *
     * @param app     Anwendung
     * @param spatial Element
     * @param dx      Startversatz x
     * @param dy      Startversatz y
     * @param delay   Verzögerung in Sekunden
     */
    public static void slideIn(Application app, Spatial spatial, float dx, float dy, float delay) {
        Animator animator = animator(app);
        if (animator == null) return;
        Vector3f target = spatial.getLocalTranslation().clone();
        spatial.setLocalTranslation(target.x + dx, target.y + dy, target.z);
        if (spatial instanceof Panel p) p.setAlpha(0);
        Tween move = Tweens.lerp(0.45f, Easing.OUT_CUBIC, t -> {
            spatial.setLocalTranslation(target.x + dx * (1 - t), target.y + dy * (1 - t), target.z);
            if (spatial instanceof Panel p) p.setAlpha(t);
        });
        animator.playUi(Tweens.seq(Tweens.delay(delay), move));
    }

    /**
     * Blendet ein Panel ein.
     *
     * @param app     Anwendung
     * @param panel   Panel
     * @param seconds Dauer
     */
    public static void fadeIn(Application app, Panel panel, float seconds) {
        Animator animator = animator(app);
        if (animator == null) return;
        panel.setAlpha(0);
        animator.playUi(Tweens.lerp(seconds, Easing.OUT_QUAD, panel::setAlpha));
    }
}
