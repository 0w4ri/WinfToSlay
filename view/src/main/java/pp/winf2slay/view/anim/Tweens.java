package pp.winf2slay.view.anim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Baukasten für Animationen: Interpolation, Folge, Parallele, Pause, Aufruf.
 *
 * <pre>{@code
 * Tweens.seq(
 *     Tweens.lerp(0.4f, Easing.OUT_CUBIC, t -> card.setLocalTranslation(start.interpolateLocal(...))),
 *     Tweens.call(sounds::cardPlace),
 *     Tweens.delay(0.2f));
 * }</pre>
 */
public final class Tweens {

    /**
     * Ein Tween ohne Dauer.
     */
    public static final Tween NONE = new Tween() {
        @Override
        public boolean update(float tpf) {
            return false;
        }

        @Override
        public void finish() {
            // nichts zu tun
        }
    };

    private Tweens() {
        // Utility-Klasse
    }

    /**
     * Empfänger eines Fortschrittswerts.
     */
    @FunctionalInterface
    public interface Progress {
        /**
         * @param t Fortschritt (nach Easing), meist von 0 bis 1
         */
        void apply(float t);
    }

    /**
     * Interpoliert über eine Dauer.
     *
     * @param seconds Dauer
     * @param easing  Verlauf
     * @param apply   wird pro Bild mit dem Fortschritt aufgerufen (zuletzt garantiert mit 1)
     * @return Tween
     */
    public static Tween lerp(float seconds, Easing easing, Progress apply) {
        return new Tween() {
            private float time;
            private boolean done;

            @Override
            public boolean update(float tpf) {
                if (done) return false;
                time += tpf;
                float t = seconds <= 0 ? 1 : Math.min(1, time / seconds);
                apply.apply(easing.apply(t));
                if (t >= 1) done = true;
                return !done;
            }

            @Override
            public void finish() {
                if (done) return;
                done = true;
                apply.apply(easing.apply(1));
            }
        };
    }

    /**
     * Lineare Interpolation über eine Dauer.
     *
     * @param seconds Dauer
     * @param apply   Empfänger des Fortschritts
     * @return Tween
     */
    public static Tween lerp(float seconds, Progress apply) {
        return lerp(seconds, Easing.LINEAR, apply);
    }

    /**
     * Wartet eine feste Zeit.
     *
     * @param seconds Dauer
     * @return Tween
     */
    public static Tween delay(float seconds) {
        return lerp(seconds, t -> {
        });
    }

    /**
     * Führt einmalig Code aus.
     *
     * @param action Aktion
     * @return Tween ohne Dauer
     */
    public static Tween call(Runnable action) {
        return new Tween() {
            private boolean done;

            @Override
            public boolean update(float tpf) {
                finish();
                return false;
            }

            @Override
            public void finish() {
                if (done) return;
                done = true;
                action.run();
            }
        };
    }

    /**
     * Erzeugt den eigentlichen Tween erst, wenn er an der Reihe ist. Damit können
     * spätere Schritte einer Folge Positionen verwenden, die erst während der
     * Animation feststehen.
     *
     * @param factory erzeugt den Tween
     * @return Tween
     */
    public static Tween defer(Supplier<Tween> factory) {
        return new Tween() {
            private Tween inner;

            private Tween inner() {
                if (inner == null) {
                    inner = factory.get();
                    if (inner == null) inner = NONE;
                }
                return inner;
            }

            @Override
            public boolean update(float tpf) {
                return inner().update(tpf);
            }

            @Override
            public void finish() {
                inner().finish();
            }
        };
    }

    /**
     * Wartet, bis eine Bedingung erfüllt ist.
     *
     * @param condition Bedingung
     * @return Tween
     */
    public static Tween waitUntil(BooleanSupplier condition) {
        return new Tween() {
            @Override
            public boolean update(float tpf) {
                return !condition.getAsBoolean();
            }

            @Override
            public void finish() {
                // Bedingung wird nicht erzwungen
            }
        };
    }

    /**
     * Führt Tweens nacheinander aus.
     *
     * @param tweens Schritte
     * @return Folge
     */
    public static Tween seq(Tween... tweens) {
        return seq(Arrays.asList(tweens));
    }

    /**
     * Führt Tweens nacheinander aus.
     *
     * @param tweens Schritte
     * @return Folge
     */
    public static Tween seq(List<Tween> tweens) {
        List<Tween> list = new ArrayList<>(tweens);
        return new Tween() {
            private int index;

            @Override
            public boolean update(float tpf) {
                while (index < list.size()) {
                    // Ein beendeter Schritt gibt die Bildzeit an den nächsten weiter, damit
                    // sofortige Schritte (Aufrufe) keine Zeit „verbrauchen“.
                    if (list.get(index).update(tpf)) return true;
                    index++;
                }
                return false;
            }

            @Override
            public void finish() {
                while (index < list.size()) {
                    list.get(index++).finish();
                }
            }
        };
    }

    /**
     * Führt Tweens gleichzeitig aus; endet mit dem längsten.
     *
     * @param tweens Teile
     * @return Parallele
     */
    public static Tween par(Tween... tweens) {
        return par(Arrays.asList(tweens));
    }

    /**
     * Führt Tweens gleichzeitig aus; endet mit dem längsten.
     *
     * @param tweens Teile
     * @return Parallele
     */
    public static Tween par(List<Tween> tweens) {
        List<Tween> running = new ArrayList<>(tweens);
        return new Tween() {
            @Override
            public boolean update(float tpf) {
                running.removeIf(t -> !t.update(tpf));
                return !running.isEmpty();
            }

            @Override
            public void finish() {
                for (Tween t : running) t.finish();
                running.clear();
            }
        };
    }

    /**
     * Startet die Teile versetzt um {@code stagger} Sekunden.
     *
     * @param stagger Versatz zwischen den Starts
     * @param tweens  Teile
     * @return Parallele mit Versatz
     */
    public static Tween stagger(float stagger, List<Tween> tweens) {
        List<Tween> parts = new ArrayList<>();
        for (int i = 0; i < tweens.size(); i++) {
            parts.add(i == 0 ? tweens.get(i) : seq(delay(stagger * i), tweens.get(i)));
        }
        return par(parts);
    }
}
