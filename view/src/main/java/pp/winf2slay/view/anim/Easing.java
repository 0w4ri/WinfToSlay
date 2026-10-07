package pp.winf2slay.view.anim;

import com.jme3.math.FastMath;

/**
 * Easing-Funktionen für Animationen. Alle Funktionen bilden 0 auf 0 und 1 auf 1 ab.
 */
@FunctionalInterface
public interface Easing {

    /**
     * Gleichmäßige Bewegung.
     */
    Easing LINEAR = t -> t;

    /**
     * Langsamer Start.
     */
    Easing IN_QUAD = t -> t * t;

    /**
     * Langsames Ende.
     */
    Easing OUT_QUAD = t -> 1 - (1 - t) * (1 - t);

    /**
     * Weiches Anfahren und Abbremsen.
     */
    Easing IN_OUT_CUBIC = t -> t < 0.5f ? 4 * t * t * t : 1 - FastMath.pow(-2 * t + 2, 3) / 2;

    /**
     * Schnell, dann deutlich abbremsend.
     */
    Easing OUT_CUBIC = t -> 1 - FastMath.pow(1 - t, 3);

    /**
     * Beschleunigend (z. B. für fallende Objekte).
     */
    Easing IN_CUBIC = t -> t * t * t;

    /**
     * Schießt leicht über das Ziel hinaus und federt zurück.
     */
    Easing OUT_BACK = t -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        float u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    };

    /**
     * Elastisches Nachschwingen.
     */
    Easing OUT_ELASTIC = t -> {
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        return FastMath.pow(2, -10 * t) * FastMath.sin((t * 10 - 0.75f) * FastMath.TWO_PI / 3) + 1;
    };

    /**
     * Aufprallen mit Abprallern.
     */
    Easing OUT_BOUNCE = t -> {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        if (t < 1 / d1) return n1 * t * t;
        if (t < 2 / d1) {
            t -= 1.5f / d1;
            return n1 * t * t + 0.75f;
        }
        if (t < 2.5 / d1) {
            t -= 2.25f / d1;
            return n1 * t * t + 0.9375f;
        }
        t -= 2.625f / d1;
        return n1 * t * t + 0.984375f;
    };

    /**
     * Sinusförmiges An- und Abschwellen (0 → 1 → 0), z. B. für Pulse.
     */
    Easing PULSE = t -> FastMath.sin(t * FastMath.PI);

    /**
     * Bildet den linearen Fortschritt auf den geglätteten ab.
     *
     * @param t Fortschritt von 0 bis 1
     * @return geglätteter Fortschritt
     */
    float apply(float t);
}
