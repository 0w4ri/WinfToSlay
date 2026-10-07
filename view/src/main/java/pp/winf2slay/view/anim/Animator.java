package pp.winf2slay.view.anim;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import pp.winf2slay.controller.client.AnimationGate;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Spielt alle Animationen ab und dient als {@link AnimationGate} der Client-Logik.
 *
 * <p>Es gibt drei Spuren:</p>
 * <ul>
 *     <li><b>Hauptspur</b> ({@link #enqueue(Tween)}): Spielereignisse laufen
 *     nacheinander ab. Erst wenn die Hauptspur leer ist, wird dem Server der
 *     Abschluss gemeldet.</li>
 *     <li><b>Effekte</b> ({@link #play(Tween)}): Begleitende Animationen
 *     (Partikel, Glanz), die niemanden aufhalten.</li>
 *     <li><b>Oberfläche</b> ({@link #playUi(Tween)}): Menüs und Einblendungen –
 *     unabhängig vom eingestellten Animationstempo.</li>
 * </ul>
 *
 * <p>Ein Watchdog beendet hängende Hauptspur-Animationen spätestens nach
 * {@value #WATCHDOG_SECONDS} Sekunden, damit das Spiel nie blockiert.</p>
 */
public class Animator extends BaseAppState implements AnimationGate {

    private static final Logger LOGGER = System.getLogger(Animator.class.getName());

    /**
     * Höchstdauer der Hauptspur ohne Fortschritt (echte Sekunden).
     */
    public static final float WATCHDOG_SECONDS = 20f;

    private final Deque<Tween> mainTrack = new ArrayDeque<>();
    private final List<Tween> effects = new ArrayList<>();
    private final List<Tween> ui = new ArrayList<>();
    private final List<Runnable> idleCallbacks = new ArrayList<>();
    private float speed = 1f;
    private float boost = 1f;
    private float busyTime;

    @Override
    protected void initialize(Application app) {
        // nichts zu tun
    }

    @Override
    protected void cleanup(Application app) {
        clear();
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
     * @param speed Tempofaktor aus den Einstellungen (1 = normal)
     */
    public void setSpeed(float speed) {
        this.speed = Math.max(0.1f, speed);
    }

    /**
     * @return Tempofaktor
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Zusätzliche Beschleunigung, z. B. solange die Leertaste gehalten wird.
     *
     * @param boost Faktor (1 = keine)
     */
    public void setBoost(float boost) {
        this.boost = Math.max(1f, boost);
    }

    /**
     * Hängt ein Spielereignis an die Hauptspur an.
     *
     * @param tween Animation
     */
    public void enqueue(Tween tween) {
        mainTrack.addLast(tween);
    }

    /**
     * Spielt eine begleitende Animation ab (blockiert nicht).
     *
     * @param tween Animation
     */
    public void play(Tween tween) {
        effects.add(tween);
    }

    /**
     * Spielt eine Oberflächenanimation in Echtzeit ab.
     *
     * @param tween Animation
     */
    public void playUi(Tween tween) {
        ui.add(tween);
    }

    /**
     * @return {@code true}, solange die Hauptspur Animationen enthält
     */
    public boolean isBusy() {
        return !mainTrack.isEmpty();
    }

    /**
     * Führt {@code action} aus, sobald die Hauptspur leer ist (sofort, falls sie es schon ist).
     *
     * @param action Aktion
     */
    public void whenIdle(Runnable action) {
        if (mainTrack.isEmpty())
            action.run();
        else
            idleCallbacks.add(action);
    }

    @Override
    public void afterAnimations(Runnable done) {
        whenIdle(done);
    }

    /**
     * Beendet alle laufenden Animationen sofort im Endzustand.
     */
    public void finishAll() {
        while (!mainTrack.isEmpty()) {
            mainTrack.pollFirst().finish();
        }
        for (Tween t : new ArrayList<>(effects)) t.finish();
        effects.clear();
        runIdleCallbacks();
    }

    /**
     * Verwirft alle Animationen ohne Endzustand (beim Verlassen eines Spiels).
     */
    public void clear() {
        mainTrack.clear();
        effects.clear();
        idleCallbacks.clear();
        busyTime = 0;
    }

    @Override
    public void update(float tpf) {
        float scaled = tpf * speed * boost;
        updateList(ui, tpf);
        updateList(effects, scaled);
        updateMainTrack(tpf, scaled);
    }

    private void updateMainTrack(float realTpf, float scaled) {
        if (mainTrack.isEmpty()) {
            busyTime = 0;
            return;
        }
        busyTime += realTpf;
        if (busyTime > WATCHDOG_SECONDS) {
            LOGGER.log(Level.WARNING, "Animationen hängen seit {0} s – werden beendet", busyTime);
            while (!mainTrack.isEmpty()) {
                finishSafely(mainTrack.pollFirst());
            }
        }
        while (!mainTrack.isEmpty()) {
            Tween head = mainTrack.peekFirst();
            boolean running;
            try {
                running = head.update(scaled);
            }
            catch (RuntimeException e) {
                LOGGER.log(Level.ERROR, "Fehler in einer Animation", e);
                running = false;
            }
            if (running) break;
            mainTrack.pollFirst();
            busyTime = 0;
        }
        if (mainTrack.isEmpty()) runIdleCallbacks();
    }

    private void runIdleCallbacks() {
        while (!idleCallbacks.isEmpty() && mainTrack.isEmpty()) {
            List<Runnable> callbacks = new ArrayList<>(idleCallbacks);
            idleCallbacks.clear();
            for (Runnable r : callbacks) {
                try {
                    r.run();
                }
                catch (RuntimeException e) {
                    LOGGER.log(Level.ERROR, "Fehler nach Animationsende", e);
                }
            }
        }
    }

    private static void updateList(List<Tween> list, float tpf) {
        if (list.isEmpty()) return;
        for (Tween t : new ArrayList<>(list)) {
            boolean running;
            try {
                running = t.update(tpf);
            }
            catch (RuntimeException e) {
                LOGGER.log(Level.ERROR, "Fehler in einer Animation", e);
                running = false;
            }
            if (!running) list.remove(t);
        }
    }

    private static void finishSafely(Tween tween) {
        try {
            tween.finish();
        }
        catch (RuntimeException e) {
            LOGGER.log(Level.ERROR, "Fehler beim Beenden einer Animation", e);
        }
    }
}
