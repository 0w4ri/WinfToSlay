package pp.winf2slay.view.ui;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import pp.winf2slay.view.audio.Sfx;

import java.util.function.Consumer;

/**
 * Ein-/Aus-Schalter mit Häkchen und Beschriftung.
 */
public class Toggle extends Button {

    private final Theme theme;
    private boolean checked;

    /**
     * @param ui       Oberflächenfabrik
     * @param text     Beschriftung
     * @param checked  Startzustand
     * @param onChange Beobachter
     */
    public Toggle(Ui ui, String text, boolean checked, Consumer<Boolean> onChange) {
        super(text, Theme.STYLE);
        setName("toggle:" + text);
        this.theme = ui.theme();
        this.checked = checked;
        setFont(theme.bodyFont());
        setFontSize(Theme.SIZE_TEXT);
        setColor(Theme.CREAM.clone());
        setHighlightColor(Theme.GOLD_LIGHT.clone());
        // unsichtbarer Hintergrund: Die ganze Zeile ist anklickbar, nicht nur Text und Kästchen
        setBackground(new QuadBackgroundComponent(new ColorRGBA(0, 0, 0, 0)));
        setTextVAlignment(VAlignment.Center);
        setInsets(new Insets3f(4, 4, 4, 4));
        refresh();
        addClickCommands(source -> {
            ui.sounds().play(Sfx.CLICK);
            this.checked = !this.checked;
            refresh();
            onChange.accept(this.checked);
        });
    }

    private void refresh() {
        IconComponent icon = new IconComponent(theme.texture(checked ? "ui/check_on.png" : "ui/check_off.png"),
                                               new Vector2f(1, 1), 0, 0, 0.02f, false);
        icon.setIconSize(new Vector2f(40, 40));
        icon.setHAlignment(HAlignment.Left);
        icon.setVAlignment(VAlignment.Center);
        icon.setMargin(8, 0);
        setIcon(icon);
    }

    /**
     * @return Zustand
     */
    public boolean isChecked() {
        return checked;
    }
}
