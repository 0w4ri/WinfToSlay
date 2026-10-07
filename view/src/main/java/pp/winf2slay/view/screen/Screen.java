package pp.winf2slay.view.screen;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.event.DefaultMouseListener;
import pp.winf2slay.view.WinfToSlayApp;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.Ui;
import pp.winf2slay.view.ui.UiScale;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Basisklasse aller Menübildschirme.
 *
 * <p>Ein Bildschirm baut seine Oberfläche in Referenzpixeln (1920 × 1080) in
 * {@link #build(Node, float, float)} auf; die Skalierung auf die Fenstergröße und
 * der Neuaufbau bei Größenänderungen passieren hier. Als Overlay (z. B.
 * Einstellungen im laufenden Spiel) wird zusätzlich der Hintergrund abgedunkelt.</p>
 */
public abstract class Screen extends BaseAppState {

    private static final Logger LOGGER = System.getLogger(Screen.class.getName());

    private final Node root = new Node(getClass().getSimpleName());
    private final boolean overlay;
    private WinfToSlayApp app;
    private float lastWidth;
    private float lastHeight;

    /**
     * @param overlay {@code true}, wenn der Bildschirm über einer anderen Ansicht liegt
     */
    protected Screen(boolean overlay) {
        this.overlay = overlay;
    }

    /**
     * @return Anwendung
     */
    protected WinfToSlayApp app() {
        return app;
    }

    /**
     * @return Oberflächenfabrik
     */
    protected Ui ui() {
        return app.getUi();
    }

    @Override
    protected void initialize(Application application) {
        this.app = (WinfToSlayApp) application;
    }

    @Override
    protected void cleanup(Application application) {
        root.removeFromParent();
    }

    @Override
    protected void onEnable() {
        app.getGuiNode().attachChild(root);
        rebuild();
    }

    @Override
    protected void onDisable() {
        root.removeFromParent();
    }

    /**
     * Baut die Oberfläche neu auf (z. B. nach Größenänderung oder neuen Daten).
     */
    protected void rebuild() {
        root.detachAllChildren();
        float s = UiScale.of(app.getCamera());
        root.setLocalScale(s);
        root.setLocalTranslation(0, 0, overlay ? 70 : 20);
        float w = UiScale.width(app.getCamera());
        float h = UiScale.height(app.getCamera());
        lastWidth = app.getCamera().getWidth();
        lastHeight = app.getCamera().getHeight();
        if (overlay) {
            Panel shade = new Panel(w, h, Theme.STYLE);
            shade.setBackground(ui().theme().solid(Theme.OVERLAY));
            shade.setLocalTranslation(0, h, -5);
            shade.addMouseListener(new DefaultMouseListener() {
                @Override
                public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture) {
                    event.setConsumed();
                }
            });
            root.attachChild(shade);
        }
        Node content = new Node("content");
        root.attachChild(content);
        try {
            build(content, w, h);
        }
        catch (RuntimeException e) {
            // Ein fehlerhafter Bildschirm darf das Programm nicht lahmlegen: zurück ins Hauptmenü.
            LOGGER.log(Level.ERROR, "Bildschirm " + getClass().getSimpleName() + " konnte nicht aufgebaut werden", e);
            content.detachAllChildren();
            if (!(this instanceof MainMenuScreen)) {
                app.enqueue(() -> app.showError("Unerwarteter Fehler",
                                                "Dieser Bildschirm konnte nicht geöffnet werden (" + e + ")."));
            }
        }
    }

    /**
     * Baut die Oberfläche auf.
     *
     * @param root   Knoten in Referenzpixeln (Ursprung unten links)
     * @param width  Breite in Referenzpixeln
     * @param height Höhe in Referenzpixeln
     */
    protected abstract void build(Node root, float width, float height);

    /**
     * Zentriert ein Panel.
     *
     * @param panel  Panel
     * @param width  Bildschirmbreite (Referenz)
     * @param height Bildschirmhöhe (Referenz)
     * @param dy     Verschiebung nach oben
     */
    protected static void center(Panel panel, float width, float height, float dy) {
        Vector3f size = panel.getPreferredSize();
        panel.setLocalTranslation((width - size.x) / 2, (height + size.y) / 2 + dy, 0);
    }

    /**
     * Wird bei Esc aufgerufen.
     */
    public void onBack() {
        // Standard: nichts
    }

    /**
     * Wird vor einem Neuaufbau wegen geänderter Fenstergröße aufgerufen, damit
     * Bildschirme Eingaben (Textfelder) sichern können.
     */
    protected void beforeResize() {
        // Standard: nichts zu sichern
    }

    @Override
    public void update(float tpf) {
        if (app.getCamera().getWidth() != lastWidth || app.getCamera().getHeight() != lastHeight) {
            beforeResize();
            rebuild();
        }
    }
}
