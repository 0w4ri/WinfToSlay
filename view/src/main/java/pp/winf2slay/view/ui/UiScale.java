package pp.winf2slay.view.ui;

import com.jme3.renderer.Camera;

/**
 * Umrechnung zwischen Referenzauflösung (1920 × 1080) und Fenstergröße.
 *
 * <p>Alle Oberflächen werden in Referenzpixeln gebaut und anschließend als Ganzes
 * skaliert. Dadurch sieht die Oberfläche in jeder Fenstergröße gleich aus.</p>
 */
public final class UiScale {

    /** Referenzbreite. */
    public static final float REF_WIDTH = 1920f;
    /** Referenzhöhe. */
    public static final float REF_HEIGHT = 1080f;

    private UiScale() {
        // Utility-Klasse
    }

    /**
     * @param cam Kamera (liefert die Fenstergröße)
     * @return Skalierungsfaktor Referenz → Fenster
     */
    public static float of(Camera cam) {
        return Math.max(0.35f, Math.min(cam.getWidth() / REF_WIDTH, cam.getHeight() / REF_HEIGHT));
    }

    /**
     * @param cam Kamera
     * @return Fensterbreite in Referenzpixeln
     */
    public static float width(Camera cam) {
        return cam.getWidth() / of(cam);
    }

    /**
     * @param cam Kamera
     * @return Fensterhöhe in Referenzpixeln
     */
    public static float height(Camera cam) {
        return cam.getHeight() / of(cam);
    }
}
