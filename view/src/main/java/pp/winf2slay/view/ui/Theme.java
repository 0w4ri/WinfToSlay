package pp.winf2slay.view.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.Styles;

/**
 * Farben, Schriften und Hintergründe der Oberfläche („Taverne“: dunkles Holz,
 * Ziegelrot, Gold, Creme – passend zum Logo).
 *
 * <p>Alle Maße der Oberfläche beziehen sich auf eine Referenzauflösung von
 * 1920 × 1080 Pixeln und werden von {@link UiScale} an das Fenster angepasst.</p>
 */
public final class Theme {

    /**
     * Name des Lemur-Stils.
     */
    public static final String STYLE = "winf";

    /** Helle Schriftfarbe. */
    public static final ColorRGBA CREAM = rgb(244, 235, 217);
    /** Gedämpfte Schriftfarbe. */
    public static final ColorRGBA MUTED = rgb(190, 172, 150);
    /** Gold (Hervorhebungen). */
    public static final ColorRGBA GOLD = rgb(242, 179, 61);
    /** Helles Gold. */
    public static final ColorRGBA GOLD_LIGHT = rgb(255, 214, 120);
    /** Ziegelrot. */
    public static final ColorRGBA RED = rgb(214, 64, 48);
    /** Grün (Erfolg). */
    public static final ColorRGBA GREEN = rgb(110, 200, 100);
    /** Blau (Information, Magie). */
    public static final ColorRGBA BLUE = rgb(110, 180, 255);
    /** Lila (Zauber). */
    public static final ColorRGBA PURPLE = rgb(186, 120, 255);
    /** Dunkle Kontur. */
    public static final ColorRGBA DARK = rgb(30, 15, 9);
    /** Abdunklung hinter Dialogen. */
    public static final ColorRGBA OVERLAY = new ColorRGBA(0.03f, 0.015f, 0.01f, 0.72f);

    /**
     * Spielerfarben (Platz 0 ist immer der eigene Platz).
     */
    private static final ColorRGBA[] PLAYER_COLORS = {
            rgb(242, 179, 61),   // Gold
            rgb(72, 196, 186),   // Türkis
            rgb(176, 120, 236),  // Violett
            rgb(130, 204, 92),   // Grün
            rgb(240, 110, 70),   // Orange
            rgb(96, 168, 240),   // Blau
    };

    /** Schriftgrößen (Referenzpixel). */
    public static final float SIZE_TITLE = 96;
    /** Überschrift. */
    public static final float SIZE_HEADING = 46;
    /** Schaltflächen. */
    public static final float SIZE_BUTTON = 30;
    /** Normaler Text. */
    public static final float SIZE_TEXT = 26;
    /** Kleiner Text. */
    public static final float SIZE_SMALL = 21;

    private final AssetManager assets;
    private final BitmapFont titleFont;
    private final BitmapFont headingFont;
    private final BitmapFont uiFont;
    private final BitmapFont bodyFont;

    /**
     * Lädt Schriften und richtet den Lemur-Stil ein. Muss nach
     * {@code GuiGlobals.initialize(app)} aufgerufen werden.
     *
     * @param assets Asset-Manager
     */
    public Theme(AssetManager assets) {
        this.assets = assets;
        titleFont = assets.loadFont("fonts/title.fnt");
        headingFont = assets.loadFont("fonts/heading.fnt");
        uiFont = assets.loadFont("fonts/ui.fnt");
        bodyFont = assets.loadFont("fonts/body.fnt");
        setupStyles();
    }

    private static ColorRGBA rgb(int r, int g, int b) {
        return new ColorRGBA(r / 255f, g / 255f, b / 255f, 1f);
    }

    /**
     * @param seat Platznummer (0 = eigener Platz)
     * @return Farbe des Platzes
     */
    public static ColorRGBA playerColor(int seat) {
        return PLAYER_COLORS[Math.floorMod(seat, PLAYER_COLORS.length)].clone();
    }

    /**
     * @param color Farbe
     * @param alpha Deckkraft
     * @return Kopie mit neuer Deckkraft
     */
    public static ColorRGBA withAlpha(ColorRGBA color, float alpha) {
        ColorRGBA c = color.clone();
        c.a = alpha;
        return c;
    }

    private void setupStyles() {
        Styles styles = GuiGlobals.getInstance().getStyles();
        Attributes base = styles.getSelector(STYLE);
        base.set("font", uiFont);
        base.set("fontSize", SIZE_TEXT);
        base.set("color", CREAM);


        Attributes field = styles.getSelector("textField", STYLE);
        field.set("font", bodyFont);
        field.set("fontSize", 30f);
        field.set("color", CREAM);
        field.set("background", field());
        field.set("singleLine", true);

        styles.setDefaultStyle(STYLE);
    }

    // ------------------------------------------------------------------
    // Schriften
    // ------------------------------------------------------------------

    /** @return große Titelschrift mit Kontur */
    public BitmapFont titleFont() {
        return titleFont;
    }

    /** @return Überschriftenschrift mit Kontur */
    public BitmapFont headingFont() {
        return headingFont;
    }

    /** @return Schrift für Schaltflächen und Beschriftungen (mit leichter Kontur) */
    public BitmapFont uiFont() {
        return uiFont;
    }

    /** @return Fließtextschrift ohne Kontur */
    public BitmapFont bodyFont() {
        return bodyFont;
    }

    // ------------------------------------------------------------------
    // Texturen und Hintergründe
    // ------------------------------------------------------------------

    /**
     * Lädt eine Oberflächentextur (ohne Mipmaps, ohne Spiegelung).
     *
     * @param path Pfad relativ zu den Ressourcen
     * @return Textur
     */
    public Texture texture(String path) {
        return GuiGlobals.getInstance().loadTexture(path, false, false);
    }

    /**
     * @param name Name eines Material-Icons (z. B. {@code settings})
     * @return Symboltextur
     */
    public Texture icon(String name) {
        return texture("ui/icons/" + name + ".png");
    }

    /**
     * Neun-Felder-Hintergrund. Achtung: In Lemur liegen „Insets“ außerhalb des
     * Hintergrunds; der Innenabstand wird deshalb über den Rand des Hintergrunds gesetzt.
     */
    private TbtQuadBackgroundComponent nineSlice(String path, int x1, int y1, int x2, int y2, float padX,
                                                 float padY) {
        TbtQuadBackgroundComponent c = TbtQuadBackgroundComponent.create(texture(path), 0.5f, x1, y1, x2, y2, 0.01f,
                                                                         false);
        c.setMargin(padX, padY);
        return c;
    }

    /** @return Holzpanel mit goldenem Rand */
    public TbtQuadBackgroundComponent panel() {
        return nineSlice("ui/panel.png", 46, 46, 146, 146, 44, 36);
    }

    /** @return dunkles, schlichtes Panel */
    public TbtQuadBackgroundComponent darkPanel() {
        return nineSlice("ui/panel_dark.png", 32, 32, 96, 96, 16, 11);
    }

    /** @return halbtransparentes Panel für Einblendungen über der Szene */
    public TbtQuadBackgroundComponent glass() {
        return nineSlice("ui/panel_glass.png", 26, 26, 70, 70, 16, 11);
    }

    /** @return Hintergrund für Hinweise */
    public TbtQuadBackgroundComponent toast() {
        return nineSlice("ui/toast.png", 18, 18, 46, 46, 22, 11);
    }

    /** @return Hintergrund für Eingabefelder */
    public TbtQuadBackgroundComponent field() {
        return nineSlice("ui/field.png", 22, 22, 74, 58, 18, 9);
    }

    /** @return Hintergrund für Eingabefelder mit Fokus */
    public TbtQuadBackgroundComponent fieldFocus() {
        return nineSlice("ui/field_focus.png", 22, 22, 74, 58, 18, 9);
    }

    /**
     * @param variant Variante
     * @param state   {@code ""}, {@code "_hover"} oder {@code "_pressed"}
     * @return Schaltflächenhintergrund
     */
    public TbtQuadBackgroundComponent button(ButtonVariant variant, String state) {
        return nineSlice("ui/button_" + variant.getTexture() + state + ".png", 34, 34, 94, 62, 24, 12);
    }

    /**
     * @param variant Farbvariante
     * @param state   {@code ""}, {@code "_hover"} oder {@code "_pressed"}
     * @return Textur einer Schaltfläche (alle Zustände haben dieselbe Größe)
     */
    public Texture buttonTexture(ButtonVariant variant, String state) {
        return texture("ui/button_" + variant.getTexture() + state + ".png");
    }

    /** @return Textur einer deaktivierten Schaltfläche */
    public Texture buttonDisabledTexture() {
        return texture("ui/button_disabled.png");
    }

    /** @return Hintergrund einer deaktivierten Schaltfläche */
    public TbtQuadBackgroundComponent buttonDisabled() {
        return nineSlice("ui/button_disabled.png", 34, 34, 94, 62, 24, 12);
    }

    /**
     * @param hover {@code true} für den Hover-Zustand
     * @return runder Schaltflächenhintergrund
     */
    public QuadBackgroundComponent roundButton(boolean hover) {
        return new QuadBackgroundComponent(texture(hover ? "ui/button_round_hover.png" : "ui/button_round.png"));
    }

    /** @return Schiene eines Schiebereglers */
    public TbtQuadBackgroundComponent sliderTrack() {
        return nineSlice("ui/slider_track.png", 16, 12, 48, 16, 0, 0);
    }

    /** @return gefüllter Teil eines Schiebereglers */
    public TbtQuadBackgroundComponent sliderFill() {
        return nineSlice("ui/slider_fill.png", 16, 12, 48, 16, 0, 0);
    }

    /**
     * @param color Farbe
     * @return einfarbiger Hintergrund
     */
    public QuadBackgroundComponent solid(ColorRGBA color) {
        return new QuadBackgroundComponent(color.clone());
    }

    /**
     * @return Asset-Manager
     */
    public AssetManager assets() {
        return assets;
    }
}
