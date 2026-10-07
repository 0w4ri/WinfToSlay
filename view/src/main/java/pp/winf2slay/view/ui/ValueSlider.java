package pp.winf2slay.view.ui;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.core.AbstractGuiControlListener;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.event.CursorButtonEvent;
import com.simsilica.lemur.event.CursorEventControl;
import com.simsilica.lemur.event.CursorMotionEvent;
import com.simsilica.lemur.event.DefaultCursorListener;

import java.util.function.Consumer;

/**
 * Schieberegler für Werte zwischen 0 und 1 im Stil des Spiels.
 */
public class ValueSlider extends Panel {

    private static final float HEIGHT = 44;
    private static final float TRACK = 26;
    private static final float THUMB = 44;

    private final Panel fill;
    private final Panel thumb;
    private final Consumer<Float> onChange;
    private float value;
    private boolean dragging;

    /**
     * @param theme    Gestaltung
     * @param width    Breite
     * @param value    Startwert 0..1
     * @param onChange wird bei jeder Änderung aufgerufen
     */
    public ValueSlider(Theme theme, float width, float value, Consumer<Float> onChange) {
        super(width, HEIGHT, Theme.STYLE);
        this.onChange = onChange;
        this.value = clamp(value);
        setBackground(null);
        setPreferredSize(new Vector3f(width, HEIGHT, 0));

        Panel track = new Panel(width, TRACK, Theme.STYLE);
        track.setBackground(theme.sliderTrack());
        track.setSize(new Vector3f(width, TRACK, 0));
        track.setLocalTranslation(0, -(HEIGHT - TRACK) / 2, 0.01f);
        attachChild(track);

        fill = new Panel(1, TRACK, Theme.STYLE);
        fill.setBackground(theme.sliderFill());
        attachChild(fill);

        thumb = new Panel(THUMB, THUMB, Theme.STYLE);
        thumb.setBackground(new QuadBackgroundComponent(theme.texture("ui/slider_thumb.png")));
        thumb.setSize(new Vector3f(THUMB, THUMB, 0));
        attachChild(thumb);

        getControl(GuiControl.class).addListener(new AbstractGuiControlListener() {
            @Override
            public void reshape(GuiControl source, Vector3f pos, Vector3f size) {
                layoutParts();
            }
        });
        CursorEventControl.addListenersToSpatial(this, new DefaultCursorListener() {
            @Override
            public void cursorButtonEvent(CursorButtonEvent event, Spatial target, Spatial capture) {
                event.setConsumed();
                if (event.getButtonIndex() != 0) return;
                dragging = event.isPressed();
                if (dragging) setFromScreen(event.getX());
            }

            @Override
            public void cursorMoved(CursorMotionEvent event, Spatial target, Spatial capture) {
                if (dragging) setFromScreen(event.getX());
            }
        });
        layoutParts();
    }

    private void setFromScreen(float screenX) {
        float width = getSize().x > 0 ? getSize().x : getPreferredSize().x;
        float local = (screenX - getWorldTranslation().x) / getWorldScale().x;
        setValue((local - THUMB / 2) / Math.max(1, width - THUMB), true);
    }

    /**
     * @return aktueller Wert 0..1
     */
    public float getValue() {
        return value;
    }

    /**
     * @param value  neuer Wert
     * @param notify Beobachter benachrichtigen
     */
    public void setValue(float value, boolean notify) {
        float v = clamp(value);
        if (Math.abs(v - this.value) < 1e-4f) return;
        this.value = v;
        layoutParts();
        if (notify) onChange.accept(v);
    }

    private void layoutParts() {
        float width = getSize().x > 0 ? getSize().x : getPreferredSize().x;
        float x = value * (width - THUMB);
        float fillWidth = Math.max(TRACK, x + THUMB / 2);
        fill.setSize(new Vector3f(fillWidth, TRACK, 0));
        fill.setLocalTranslation(0, -(HEIGHT - TRACK) / 2, 0.02f);
        thumb.setLocalTranslation(x, 0, 0.03f);
    }

    private static float clamp(float v) {
        return Math.max(0, Math.min(1, v));
    }
}
