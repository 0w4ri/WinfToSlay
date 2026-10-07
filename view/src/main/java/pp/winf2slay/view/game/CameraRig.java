package pp.winf2slay.view.game;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

import java.util.Random;

/**
 * Steuert die Kamera: langsamer Rundflug im Menü, feste Spielperspektive mit
 * Zoom und leichtem Schwenk, weiche Übergänge und Kamerawackeln.
 *
 * <p>Die Kamera wird über Kugelkoordinaten um einen Blickpunkt beschrieben
 * (Gierwinkel, Neigung, Abstand). Alle Werte werden pro Bild weich an ihre
 * Zielwerte angenähert.</p>
 */
public class CameraRig extends BaseAppState {

    /** Neigung der Spielansicht in Grad. */
    public static final float GAME_PITCH = 52f;
    /** Abstand der Spielansicht. */
    public static final float GAME_DISTANCE = 110f;
    private static final Vector3f GAME_TARGET = new Vector3f(0, 0, 10f);

    private final Random random = new Random();
    private final Vector3f target = new Vector3f(GAME_TARGET);
    private final Vector3f targetGoal = new Vector3f(GAME_TARGET);
    private float yaw;
    private float yawGoal;
    private float pitch = 30f;
    private float pitchGoal = 30f;
    private float distance = 190f;
    private float distanceGoal = 190f;
    private float zoom = 1f;
    private boolean orbiting;
    private float orbitSpeed = 4f;
    private float smoothing = 2.5f;
    private float shakeTime;
    private float shakeDuration;
    private float shakeStrength;
    private boolean shakeEnabled = true;
    private Camera cam;
    private boolean focused;
    private final Vector3f savedTarget = new Vector3f();
    private float savedPitch;
    private float savedDistance;

    @Override
    protected void initialize(Application app) {
        cam = app.getCamera();
        cam.setFrustumPerspective(42f, (float) cam.getWidth() / cam.getHeight(), 1f, 2000f);
        apply(Vector3f.ZERO);
    }

    @Override
    protected void cleanup(Application app) {
        // nichts zu tun
    }

    @Override
    protected void onEnable() {
        // nichts zu tun
    }

    @Override
    protected void onDisable() {
        // nichts zu tun
    }

    /**
     * Langsamer Rundflug um den Tisch (Hintergrund des Menüs).
     */
    public void menuOrbit() {
        focused = false; // eine laufende Fokussierung (Angriff, Blitz) darf das Menü nicht überschreiben
        shakeDuration = 0;
        orbiting = true;
        orbitSpeed = 3.5f;
        pitchGoal = 24f;
        distanceGoal = 175f;
        targetGoal.set(0, 6f, 0);
        smoothing = 1.2f;
    }

    /**
     * Feste Perspektive vom eigenen Platz aus.
     *
     * @param instant ohne Übergang
     */
    public void gameView(boolean instant) {
        orbiting = false;
        focused = false;
        yawGoal = nearestYaw(0);
        pitchGoal = GAME_PITCH;
        distanceGoal = GAME_DISTANCE;
        targetGoal.set(GAME_TARGET);
        zoom = 1f;
        smoothing = instant ? 100f : 1.6f;
        if (instant) {
            yaw = yawGoal;
            pitch = pitchGoal;
            distance = distanceGoal;
            target.set(targetGoal);
        }
    }

    private float nearestYaw(float wanted) {
        float diff = ((wanted - yaw) % 360 + 540) % 360 - 180;
        return yaw + diff;
    }

    /**
     * Ändert den Zoom der Spielansicht (Mausrad).
     *
     * @param steps positive Werte zoomen hinein
     */
    public void zoom(float steps) {
        zoom = FastMath.clamp(zoom - steps * 0.06f, 0.62f, 1.22f);
        smoothing = 6f;
    }

    /**
     * Schwenkt die Spielansicht (mittlere Maustaste ziehen).
     *
     * @param deltaYaw   Änderung des Gierwinkels in Grad
     * @param deltaPitch Änderung der Neigung in Grad
     */
    public void orbit(float deltaYaw, float deltaPitch) {
        if (orbiting) return;
        yawGoal = FastMath.clamp(yawGoal + deltaYaw, -45f, 45f);
        pitchGoal = FastMath.clamp(pitchGoal + deltaPitch, 30f, 80f);
        smoothing = 10f;
    }

    /**
     * Setzt Schwenk und Zoom der Spielansicht zurück.
     */
    public void reset() {
        if (!orbiting) gameView(false);
    }

    /**
     * Richtet die Kamera vorübergehend auf einen Punkt aus (z. B. bei Angriffen).
     *
     * @param point    Blickpunkt
     * @param distance Abstand
     * @param pitch    Neigung in Grad
     */
    public void focus(Vector3f point, float distance, float pitch) {
        if (orbiting) return;
        if (!focused) {
            savedTarget.set(targetGoal);
            savedPitch = pitchGoal;
            savedDistance = distanceGoal;
        }
        focused = true;
        targetGoal.set(point);
        distanceGoal = distance;
        pitchGoal = pitch;
        smoothing = 2.4f;
    }

    /**
     * Beendet {@link #focus(Vector3f, float, float)} und kehrt zur vorherigen Ansicht zurück.
     */
    public void release() {
        if (!focused) return;
        focused = false;
        targetGoal.set(savedTarget);
        pitchGoal = savedPitch;
        distanceGoal = savedDistance;
        smoothing = 2.0f;
    }

    /**
     * @return aktuelle Kameraposition
     */
    public Vector3f getLocation() {
        return cam.getLocation().clone();
    }

    /**
     * Lässt die Kamera wackeln (Explosionen).
     *
     * @param strength Stärke (Weltkoordinaten)
     * @param seconds  Dauer
     */
    public void shake(float strength, float seconds) {
        if (!shakeEnabled) return;
        if (strength >= shakeStrength * (1 - shakeTime / Math.max(0.01f, shakeDuration))) {
            shakeStrength = strength;
            shakeDuration = seconds;
            shakeTime = 0;
        }
    }

    /**
     * @param enabled Kamerawackeln erlauben
     */
    public void setShakeEnabled(boolean enabled) {
        this.shakeEnabled = enabled;
    }

    @Override
    public void update(float tpf) {
        if (orbiting) yawGoal += orbitSpeed * tpf;
        float k = 1 - FastMath.exp(-tpf * smoothing);
        yaw += (yawGoal - yaw) * k;
        pitch += (pitchGoal - pitch) * k;
        distance += (distanceGoal * (orbiting || focused ? 1 : zoom) - distance) * k;
        target.interpolateLocal(targetGoal, k);

        Vector3f offset = Vector3f.ZERO;
        if (shakeTime < shakeDuration) {
            shakeTime += tpf;
            float fade = 1 - shakeTime / shakeDuration;
            float s = shakeStrength * fade * fade;
            offset = new Vector3f((random.nextFloat() * 2 - 1) * s, (random.nextFloat() * 2 - 1) * s * 0.6f,
                                  (random.nextFloat() * 2 - 1) * s);
        }
        apply(offset);
    }

    private void apply(Vector3f shake) {
        if (cam == null) return;
        float yr = yaw * FastMath.DEG_TO_RAD;
        float pr = pitch * FastMath.DEG_TO_RAD;
        Vector3f pos = new Vector3f(FastMath.sin(yr) * FastMath.cos(pr), FastMath.sin(pr),
                                    FastMath.cos(yr) * FastMath.cos(pr)).multLocal(distance).addLocal(target);
        cam.setLocation(pos.addLocal(shake));
        cam.lookAt(target.add(shake.mult(0.5f)), Vector3f.UNIT_Y);
    }

    /**
     * Passt das Seitenverhältnis nach einer Größenänderung an.
     */
    public void resize() {
        if (cam != null)
            cam.setFrustumPerspective(42f, (float) cam.getWidth() / cam.getHeight(), 1f, 2000f);
    }
}
