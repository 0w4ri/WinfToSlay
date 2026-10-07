package pp.winf2slay.view.ui;

import com.simsilica.lemur.event.PopupState;

/**
 * Lemur legt modale Dialoge knapp über das höchste Element der Oberfläche. Das wird
 * aus den Begrenzungsvolumen des aktuellen Bildes berechnet – wird ein Bildschirm im
 * selben Moment wie ein Dialog geöffnet (z. B. Fehlermeldung über dem Hauptmenü), ist
 * er dort noch nicht enthalten und läge anschließend über dem Dialog. Diese Variante
 * legt Dialoge daher immer mindestens auf {@value #MIN_Z}, über alle Bildschirme.
 */
public class TopPopupState extends PopupState {

    /**
     * Mindesthöhe (z) für Dialoge; Bildschirme und Overlays liegen darunter.
     */
    public static final float MIN_Z = 150f;

    /**
     * @param app Anwendung
     * @return der angemeldete Zustand (Rückfall: Lemurs eigener)
     */
    public static PopupState of(com.jme3.app.Application app) {
        PopupState state = app.getStateManager().getState(TopPopupState.class);
        return state != null ? state : com.simsilica.lemur.GuiGlobals.getInstance().getPopupState();
    }

    @Override
    protected float getMaxGuiZ() {
        return Math.max(super.getMaxGuiZ(), MIN_Z);
    }
}
