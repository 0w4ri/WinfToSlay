package pp.winf2slay.view.anim;

/**
 * Ein zeitlich ablaufender Animationsschritt.
 *
 * <p>Ein Tween wird pro Bild mit der vergangenen Zeit aufgerufen, bis er
 * {@code false} liefert. {@link #finish()} springt sofort in den Endzustand –
 * das wird beim Überspringen von Animationen und vom Watchdog genutzt. Nach
 * {@code finish()} darf {@link #update(float)} nicht mehr aufgerufen werden.</p>
 */
public interface Tween {

    /**
     * Schreitet die Animation fort.
     *
     * @param tpf vergangene Zeit in Sekunden (bereits mit dem Tempo verrechnet)
     * @return {@code true}, solange die Animation noch läuft
     */
    boolean update(float tpf);

    /**
     * Beendet die Animation sofort im Endzustand.
     */
    void finish();
}
