package pp.winf2slay.view.ui;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.SpringGridLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Auswahl aus einer festen Liste mit Pfeilen links und rechts ({@code ‹ Normal ›}).
 *
 * @param <T> Typ der Optionen
 */
public class OptionSelector<T> extends Container {

    private final List<T> options;
    private final Function<T, String> names;
    private final Consumer<T> onChange;
    private final Label value;
    private int index;

    /**
     * @param ui       Oberflächenfabrik
     * @param options  Optionen (nicht leer; einzelne Einträge dürfen {@code null} sein)
     * @param selected Startauswahl
     * @param names    Anzeigenamen
     * @param width    Breite des Wertefelds
     * @param onChange Beobachter
     */
    public OptionSelector(Ui ui, List<T> options, T selected, Function<T, String> names, float width,
                          Consumer<T> onChange) {
        super(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.Even), Theme.STYLE);
        if (options.isEmpty()) throw new IllegalArgumentException("Keine Optionen");
        // ArrayList statt List.copyOf: Optionen dürfen null sein (z. B. „Gemischt“ als Bot-Stärke)
        this.options = Collections.unmodifiableList(new ArrayList<>(options));
        this.names = names;
        this.onChange = onChange;
        this.index = Math.max(0, this.options.indexOf(selected));
        setBackground(null);

        Button left = ui.roundButton("chevron_left", 48, () -> step(-1));
        value = ui.outlined("", Theme.SIZE_BUTTON, Theme.CREAM);
        value.setTextHAlignment(HAlignment.Center);
        value.setTextVAlignment(VAlignment.Center);
        value.setPreferredSize(new Vector3f(width, 48, 0));
        Button right = ui.roundButton("chevron_right", 48, () -> step(1));
        addChild(left);
        addChild(value);
        addChild(right);
        refresh();
    }

    private void step(int delta) {
        index = Math.floorMod(index + delta, options.size());
        refresh();
        onChange.accept(options.get(index));
    }

    private void refresh() {
        value.setText(names.apply(options.get(index)));
    }

    /**
     * @return aktuelle Auswahl
     */
    public T getSelected() {
        return options.get(index);
    }
}
