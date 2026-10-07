package pp.winf2slay.view.ui;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.texture.Texture;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Button.ButtonAction;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import com.simsilica.lemur.focus.FocusChangeEvent;
import com.simsilica.lemur.focus.FocusChangeListener;
import pp.winf2slay.view.audio.Sfx;
import pp.winf2slay.view.audio.SoundManager;

import java.util.function.Predicate;

/**
 * Fabrik für einheitlich gestaltete Oberflächenelemente.
 */
public class Ui {

    private final Theme theme;
    private final SoundManager sounds;

    /**
     * @param theme  Gestaltung
     * @param sounds Sounds für Klick und Hover
     */
    public Ui(Theme theme, SoundManager sounds) {
        this.theme = theme;
        this.sounds = sounds;
    }

    /**
     * @return Gestaltung
     */
    public Theme theme() {
        return theme;
    }

    /**
     * @return Sounds
     */
    public SoundManager sounds() {
        return sounds;
    }

    // ------------------------------------------------------------------
    // Text
    // ------------------------------------------------------------------

    /**
     * @param text Titeltext
     * @param size Schriftgröße
     * @return großer Titel mit Kontur
     */
    public Label title(String text, float size) {
        Label l = new Label(text, Theme.STYLE);
        l.setFont(theme.titleFont());
        l.setFontSize(size);
        l.setColor(Theme.CREAM.clone());
        l.setTextHAlignment(HAlignment.Center);
        return l;
    }

    /**
     * @param text Überschrift
     * @return Überschrift (gold)
     */
    public Label heading(String text) {
        return heading(text, Theme.SIZE_HEADING, Theme.GOLD);
    }

    /**
     * @param text  Überschrift
     * @param size  Schriftgröße
     * @param color Farbe
     * @return Überschrift
     */
    public Label heading(String text, float size, ColorRGBA color) {
        Label l = new Label(text, Theme.STYLE);
        l.setFont(theme.headingFont());
        l.setFontSize(size);
        l.setColor(color.clone());
        return l;
    }

    /**
     * @param text Text
     * @return Fließtext
     */
    public Label text(String text) {
        return text(text, Theme.SIZE_TEXT, Theme.CREAM);
    }

    /**
     * @param text  Text
     * @param size  Schriftgröße
     * @param color Farbe
     * @return Fließtext
     */
    public Label text(String text, float size, ColorRGBA color) {
        Label l = new Label(text, Theme.STYLE);
        l.setFont(theme.bodyFont());
        l.setFontSize(size);
        l.setColor(color.clone());
        return l;
    }

    /**
     * @param text     Text
     * @param size     Schriftgröße
     * @param color    Farbe
     * @param maxWidth Umbruchbreite
     * @return umbrechender Fließtext
     */
    public Label wrapped(String text, float size, ColorRGBA color, float maxWidth) {
        Label l = text(text, size, color);
        l.setMaxWidth(maxWidth);
        return l;
    }

    /**
     * Beschriftung mit der Konturschrift (lesbar über der 3D-Szene).
     *
     * @param text  Text
     * @param size  Schriftgröße
     * @param color Farbe
     * @return Beschriftung
     */
    public Label outlined(String text, float size, ColorRGBA color) {
        Label l = new Label(text, Theme.STYLE);
        l.setFont(theme.uiFont());
        l.setFontSize(size);
        l.setColor(color.clone());
        return l;
    }

    // ------------------------------------------------------------------
    // Symbole
    // ------------------------------------------------------------------

    /**
     * @param name  Name des Symbols
     * @param size  Kantenlänge
     * @param color Farbe
     * @return Symbol als Komponente
     */
    public IconComponent iconComponent(String name, float size, ColorRGBA color) {
        IconComponent icon = new IconComponent(theme.icon(name), new Vector2f(1, 1), 0, 0, 0.02f, false);
        icon.setIconSize(new Vector2f(size, size));
        icon.setColor(color.clone());
        icon.setHAlignment(HAlignment.Center);
        icon.setVAlignment(VAlignment.Center);
        return icon;
    }

    /**
     * @param name  Name des Symbols
     * @param size  Kantenlänge
     * @param color Farbe
     * @return Symbol als eigenes Element
     */
    public Label icon(String name, float size, ColorRGBA color) {
        Label l = new Label("", Theme.STYLE);
        l.setIcon(iconComponent(name, size, color));
        l.setPreferredSize(new Vector3f(size, size, 0));
        return l;
    }

    /**
     * @param path  Bildpfad
     * @param w     Breite
     * @param h     Höhe
     * @return Bild als Panel
     */
    public Panel image(String path, float w, float h) {
        Panel p = new Panel(w, h, Theme.STYLE);
        QuadBackgroundComponent bg = new QuadBackgroundComponent(theme.texture(path));
        p.setBackground(bg);
        p.setPreferredSize(new Vector3f(w, h, 0));
        return p;
    }

    // ------------------------------------------------------------------
    // Schaltflächen
    // ------------------------------------------------------------------

    /**
     * @param text    Beschriftung
     * @param variant Farbvariante
     * @param action  Aktion beim Klick
     * @return Schaltfläche
     */
    public Button button(String text, ButtonVariant variant, Runnable action) {
        return button(text, null, variant, action);
    }

    /**
     * @param text    Beschriftung
     * @param icon    Symbol links neben dem Text oder {@code null}
     * @param variant Farbvariante
     * @param action  Aktion beim Klick
     * @return Schaltfläche
     */
    public Button button(String text, String icon, ButtonVariant variant, Runnable action) {
        Button b = new Button(text, Theme.STYLE);
        b.setName("button:" + text);
        b.setFont(theme.uiFont());
        b.setFontSize(Theme.SIZE_BUTTON);
        b.setColor(Theme.CREAM.clone());
        b.setHighlightColor(ColorRGBA.White.clone());
        b.setTextHAlignment(HAlignment.Center);
        b.setTextVAlignment(VAlignment.Center);
        if (icon != null) {
            IconComponent ic = iconComponent(icon, 34, Theme.CREAM);
            ic.setHAlignment(HAlignment.Left);
            ic.setMargin(10, 0);
            b.setIcon(ic);
        }
        installStates(b, variant);
        b.addClickCommands(source -> {
            if (Boolean.getBoolean("winf.uidump.input"))
                System.getLogger("pp.winf2slay.ui").log(System.Logger.Level.WARNING, "KLICK " + text);
            sounds.play(Sfx.CLICK, 1f, 0.05f);
            action.run();
        });
        return b;
    }

    /**
     * Zustände einer Schaltfläche. Es wird nur die Textur des Hintergrunds getauscht,
     * nicht der Hintergrund selbst: Ein neuer Hintergrund hätte bis zum nächsten Bild
     * keine gültige Lage, und ein schneller Klick (Drücken und Loslassen im selben Bild,
     * etwa bei niedriger Bildrate) würde ins Leere gehen.
     */
    @SuppressWarnings("unchecked")
    private void installStates(Button b, ButtonVariant variant) {
        TbtQuadBackgroundComponent bg = theme.button(variant, "");
        b.setBackground(bg);
        Texture normal = theme.buttonTexture(variant, "");
        Texture hover = theme.buttonTexture(variant, "_hover");
        Texture pressed = theme.buttonTexture(variant, "_pressed");
        Texture disabled = theme.buttonDisabledTexture();
        b.addCommands(ButtonAction.HighlightOn, source -> {
            bg.setTexture(hover);
            sounds.play(Sfx.HOVER, 1f, 0.1f);
        });
        b.addCommands(ButtonAction.HighlightOff, source -> bg.setTexture(source.isEnabled() ? normal : disabled));
        b.addCommands(ButtonAction.Down, source -> bg.setTexture(pressed));
        b.addCommands(ButtonAction.Up, source -> bg.setTexture(source.isHighlightOn() ? hover : normal));
        b.addCommands(ButtonAction.Disabled, source -> {
            bg.setTexture(disabled);
            source.setColor(Theme.MUTED.clone());
        });
        b.addCommands(ButtonAction.Enabled, source -> {
            bg.setTexture(normal);
            source.setColor(Theme.CREAM.clone());
        });
    }

    /**
     * Runde Symbolschaltfläche (z. B. Menü, Hilfe).
     *
     * @param icon   Symbol
     * @param size   Durchmesser
     * @param action Aktion
     * @return Schaltfläche
     */
    @SuppressWarnings("unchecked")
    public Button roundButton(String icon, float size, Runnable action) {
        Button b = new Button("", Theme.STYLE);
        b.setName("button:" + icon);
        QuadBackgroundComponent bg = theme.roundButton(false);
        Texture normal = bg.getTexture();
        Texture hover = theme.roundButton(true).getTexture();
        b.setBackground(bg);
        b.setIcon(iconComponent(icon, size * 0.55f, Theme.CREAM));
        b.setPreferredSize(new Vector3f(size, size, 0));
        b.addCommands(ButtonAction.HighlightOn, source -> {
            bg.setTexture(hover);
            sounds.play(Sfx.HOVER, 1f, 0.1f);
        });
        b.addCommands(ButtonAction.HighlightOff, source -> bg.setTexture(normal));
        b.addClickCommands(source -> {
            sounds.play(Sfx.CLICK, 1f, 0.05f);
            action.run();
        });
        return b;
    }

    // ------------------------------------------------------------------
    // Container
    // ------------------------------------------------------------------

    /**
     * @return vertikaler Container mit Holzpanel
     */
    public Container panel() {
        Container c = new Container(Theme.STYLE);
        c.setBackground(theme.panel());
        return c;
    }

    /**
     * @return halbtransparenter Container
     */
    public Container glass() {
        Container c = new Container(Theme.STYLE);
        c.setBackground(theme.glass());
        return c;
    }

    /**
     * @return Container ohne Hintergrund (vertikal)
     */
    public Container column() {
        Container c = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even), Theme.STYLE);
        c.setBackground(null);
        return c;
    }

    /**
     * @return Container ohne Hintergrund (horizontal)
     */
    public Container row() {
        Container c = new Container(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.Even), Theme.STYLE);
        c.setBackground(null);
        return c;
    }

    /**
     * @param width Breite
     * @return goldene Trennlinie
     */
    public Panel divider(float width) {
        Panel p = new Panel(width, 16, Theme.STYLE);
        p.setBackground(new QuadBackgroundComponent(theme.texture("ui/divider.png")));
        p.setPreferredSize(new Vector3f(width, 16, 0));
        return p;
    }

    /**
     * @param w Breite
     * @param h Höhe
     * @return unsichtbarer Abstandshalter
     */
    public Panel spacer(float w, float h) {
        Panel p = new Panel(w, h, Theme.STYLE);
        p.setBackground(null);
        p.setPreferredSize(new Vector3f(w, h, 0));
        return p;
    }

    /**
     * Zeile eines Formulars: Beschriftung links, Eingabe rechts.
     *
     * @param label Beschriftung
     * @param input Eingabeelement
     * @return Zeile
     */
    public Container formRow(String label, Panel input) {
        Container row = row();
        row.setName("field:" + label);
        Label l = outlined(label, 26, Theme.CREAM);
        l.setPreferredSize(new Vector3f(280, 56, 0));
        l.setTextVAlignment(VAlignment.Center);
        row.addChild(l);
        row.addChild(input);
        return row;
    }

    // ------------------------------------------------------------------
    // Eingaben
    // ------------------------------------------------------------------

    /**
     * @param text      Startwert
     * @param maxLength Höchstlänge
     * @param allowed   erlaubte Zeichen
     * @param width     Breite
     * @return Eingabefeld
     */
    public TextField textField(String text, int maxLength, Predicate<Character> allowed, float width) {
        TextField field = new TextField(new LimitedDocument(text, maxLength, allowed), Theme.STYLE);
        field.setPreferredWidth(width);
        field.getControl(com.simsilica.lemur.core.GuiControl.class).addFocusChangeListener(new FocusChangeListener() {
            @Override
            public void focusGained(FocusChangeEvent event) {
                field.setBackground(theme.fieldFocus());
            }

            @Override
            public void focusLost(FocusChangeEvent event) {
                field.setBackground(theme.field());
            }
        });
        return field;
    }

    /**
     * Setzt den Tastaturfokus auf ein Element.
     *
     * @param field Element
     */
    public void focus(Panel field) {
        GuiGlobals.getInstance().requestFocus(field);
    }

    /**
     * Wählt den passenden Container-Hintergrund für Zeilen in Listen.
     *
     * @param highlight {@code true} für hervorgehobene Zeilen
     * @return Hintergrund
     */
    public QuadBackgroundComponent rowBackground(boolean highlight) {
        QuadBackgroundComponent bg = theme.solid(highlight ? new ColorRGBA(0.95f, 0.7f, 0.24f, 0.18f)
                                                           : new ColorRGBA(0, 0, 0, 0.22f));
        bg.setMargin(14, 8);
        return bg;
    }
}
