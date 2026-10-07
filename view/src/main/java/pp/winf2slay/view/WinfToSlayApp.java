package pp.winf2slay.view;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.AppState;
import com.jme3.audio.AudioListenerState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import pp.winf2slay.controller.client.event.DisconnectedEvent;
import pp.winf2slay.controller.client.event.GameEventListener;
import pp.winf2slay.controller.client.event.GameStartedEvent;
import pp.winf2slay.controller.client.event.NameConfirmedEvent;
import pp.winf2slay.view.anim.Animator;
import pp.winf2slay.view.audio.SoundManager;
import pp.winf2slay.view.dev.FrameRecorder;
import pp.winf2slay.view.dev.Showcase;
import pp.winf2slay.view.game.CameraRig;
import pp.winf2slay.view.game.GameScreen;
import pp.winf2slay.view.game.TableScene;
import pp.winf2slay.view.game.card.CardFactory;
import pp.winf2slay.view.net.Session;
import pp.winf2slay.view.screen.HelpScreen;
import pp.winf2slay.view.screen.HostScreen;
import pp.winf2slay.view.screen.JoinScreen;
import pp.winf2slay.view.screen.LobbyScreen;
import pp.winf2slay.view.screen.MainMenuScreen;
import pp.winf2slay.view.screen.MenuBackdrop;
import pp.winf2slay.view.screen.Screen;
import pp.winf2slay.view.screen.SettingsScreen;
import pp.winf2slay.view.screen.SoloScreen;
import pp.winf2slay.view.settings.UserSettings;
import pp.winf2slay.view.ui.ButtonVariant;
import pp.winf2slay.view.ui.Dialog;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.TopPopupState;
import pp.winf2slay.view.ui.Ui;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hauptanwendung von WinfToSlay.
 *
 * <p>Verwaltet die Bildschirme (Menüs, Lobby, Spiel), die gemeinsame 3D-Szene,
 * Animationen, Sounds und die aktuelle Netzwerksitzung. Alle Methoden laufen im
 * jME-Render-Thread.</p>
 *
 * <p>Für Vorführungen und Tests: {@code -Dwinf.autostart=solo} startet direkt ein
 * Solospiel, {@code -Dwinf.autoplay=true} lässt den eigenen Platz automatisch spielen,
 * {@code -Dwinf.showcase=true} zeigt alle Animationen in einem erfundenen Spielverlauf.</p>
 */
public class WinfToSlayApp extends SimpleApplication {

    private static final String MAPPING_ESCAPE = "app.escape";

    /**
     * Starke Referenz, damit die Protokollstufe nicht vom Garbage Collector verworfen wird.
     */
    private static final Logger PROJECT_LOGGER = Logger.getLogger("pp.winf2slay");
    private static final Logger JME_CLIENT_LOGGER = Logger.getLogger("com.jme3.network.base.DefaultClient");

    private UserSettings settings;
    private Ui ui;
    private Animator animator;
    private SoundManager sounds;
    private CameraRig rig;
    private TableScene table;
    private CardFactory cards;
    private MenuBackdrop backdrop;
    private Screen screen;
    private Screen overlay;
    private GameScreen game;
    private Session session;
    private float saveTimer;
    private long errorWindowStart;
    private int errorCount;

    /**
     * Startet das Spiel.
     *
     * @param args Kommandozeilenargumente (nicht verwendet)
     */
    public static void main(String[] args) {
        Logger.getLogger("").setLevel(Level.WARNING);
        PROJECT_LOGGER.setLevel(Level.INFO);
        // jME meldet jedes reguläre Verbindungsende (z. B. Server nach Spielende beendet)
        // als SEVERE – das ist hier normaler Ablauf und wird über die Spiellogik angezeigt.
        JME_CLIENT_LOGGER.setFilter(r -> r.getMessage() == null || !r.getMessage().startsWith("Connection terminated"));
        PROJECT_LOGGER.log(Level.INFO, "WinfToSlay {0}, Java {1}",
                           new Object[]{AppInfo.VERSION, System.getProperty("java.version")});
        boolean mac = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac");
        if (mac) {
            // macOS: Das Fenster läuft im Hauptthread (-XstartOnFirstThread). AWT darf dort nicht
            // starten, sonst hängt das Programm – AWT wird nur für Bilddaten gebraucht.
            System.setProperty("java.awt.headless", "true");
        }
        UserSettings prefs = new UserSettings();
        WinfToSlayApp app = new WinfToSlayApp(prefs);
        AppSettings s = new AppSettings(true);
        s.setTitle(System.getProperty("winf.title", "WinfToSlay"));
        s.setResolution(Integer.getInteger("winf.width", 1600), Integer.getInteger("winf.height", 900));
        s.setResizable(true);
        s.setVSync(true);
        s.setGammaCorrection(false);
        s.setSamples(prefs.getQuality() == UserSettings.Quality.HIGH ? 4 : 0);
        if (!mac) s.setIcons(loadIcons()); // macOS: Symbol kommt aus dem Programmpaket
        app.setSettings(s);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.start();
    }

    private static Object[] loadIcons() {
        List<BufferedImage> icons = new ArrayList<>();
        for (int size : new int[]{128, 64, 32, 16}) {
            try (InputStream in = WinfToSlayApp.class.getResourceAsStream("/images/icon" + size + ".png")) {
                if (in != null) icons.add(ImageIO.read(in));
            }
            catch (IOException e) {
                // Fenster ohne Symbol
            }
        }
        return icons.toArray();
    }

    /**
     * @param settings Benutzereinstellungen
     */
    public WinfToSlayApp(UserSettings settings) {
        super(new StatsAppState(), new AudioListenerState());
        this.settings = settings;
    }

    @Override
    public void simpleInitApp() {
        if (inputManager.hasMapping(INPUT_MAPPING_EXIT)) inputManager.deleteMapping(INPUT_MAPPING_EXIT);
        setDisplayStatView(false);
        setDisplayFps(settings.isShowFps());
        viewPort.setBackgroundColor(new ColorRGBA(0.05f, 0.03f, 0.02f, 1f));

        if (Boolean.getBoolean("winf.uidump.input"))
            inputManager.addRawInputListener(pp.winf2slay.view.dev.UiDump.inputLogger());
        GuiGlobals.initialize(this);
        // Dialoge immer über allen Bildschirmen (siehe TopPopupState)
        stateManager.attach(new TopPopupState());
        Theme theme = new Theme(assetManager);
        animator = new Animator();
        sounds = new SoundManager(settings);
        rig = new CameraRig();
        stateManager.attachAll(animator, sounds, rig);
        ui = new Ui(theme, sounds);
        cards = new CardFactory(assetManager);
        table = new TableScene(this);
        table.build(settings.getQuality());
        backdrop = new MenuBackdrop();
        stateManager.attach(backdrop);
        applySettings();
        settings.addListener(this::applySettings);

        inputManager.addMapping(MAPPING_ESCAPE, new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addListener((ActionListener) (name, pressed, tpf) -> {
            if (pressed) onEscape();
        }, MAPPING_ESCAPE);

        String uiDump = System.getProperty("winf.uidump");
        if (uiDump != null) stateManager.attach(new pp.winf2slay.view.dev.UiDump(java.nio.file.Path.of(uiDump)));

        String record = System.getProperty("winf.record");
        if (record != null)
            stateManager.attach(new FrameRecorder(new java.io.File(record), Integer.getInteger("winf.recordEvery", 4), 20));

        String autostart = System.getProperty("winf.autostart", "");
        if (Boolean.getBoolean("winf.fxtest")) {
            backdrop.setEnabled(false);
            stateManager.attach(new pp.winf2slay.view.dev.FxTest());
        }
        else if (Boolean.getBoolean("winf.showcase")) {
            Showcase showcase = new Showcase(this);
            stateManager.attach(showcase);
            showcase.begin();
        }
        else if ("solo".equalsIgnoreCase(autostart))
            startSolo(settings.getPlayerName());
        else
            showMainMenu();
    }

    private void applySettings() {
        animator.setSpeed(settings.getAnimationSpeed().getFactor());
        table.applyQuality(settings.getQuality());
        rig.setShakeEnabled(settings.isCameraShake());
        setDisplayFps(settings.isShowFps());
        if (session != null) session.setBotSpeed(settings.getBotSpeed());
    }

    @Override
    public void simpleUpdate(float tpf) {
        saveTimer += tpf;
        if (saveTimer > 2f) {
            saveTimer = 0;
            settings.save();
        }
    }

    /**
     * Fängt Fehler eines Bildes ab, damit das Programm nicht beendet wird: Ohne
     * diesen Schutz würde jede Ausnahme (z. B. in einem Klick) den Render-Thread
     * beenden. Der erste Fehler zeigt einen Hinweis, eine Fehlerserie führt zurück
     * ins Hauptmenü, und erst wenn auch das nicht hilft, endet das Programm.
     */
    @Override
    public void update() {
        try {
            super.update();
        }
        catch (RuntimeException e) {
            recoverFrom(e);
        }
    }

    private void recoverFrom(RuntimeException e) {
        long now = System.currentTimeMillis();
        if (now - errorWindowStart > 10_000) {
            errorWindowStart = now;
            errorCount = 0;
        }
        errorCount++;
        PROJECT_LOGGER.log(Level.SEVERE, "Unerwarteter Fehler", e);
        if (errorCount > 8) throw e; // dauerhaft defekt → Standardbehandlung von jME
        String detail = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
        if (errorCount == 1) {
            enqueue(() -> new Dialog(ui, this, "Unerwarteter Fehler",
                                     "Etwas ist schiefgelaufen (" + detail + "). Du kannst weiterspielen "
                                     + "oder zum Hauptmenü zurückkehren. Details stehen im Log.")
                    .addButton("Weiter", ButtonVariant.SECONDARY, null)
                    .addButton("Zum Hauptmenü", ButtonVariant.PRIMARY, () -> {
                        leaveSession();
                        showMainMenu();
                    })
                    .show());
        }
        else if (errorCount == 4) {
            enqueue(() -> showError("Unerwarteter Fehler",
                                    "Der Fehler tritt wiederholt auf (" + detail + "). Das Spiel wurde beendet."));
        }
    }

    @Override
    public void reshape(int w, int h) {
        super.reshape(w, h);
        if (rig != null) rig.resize();
    }

    @Override
    public void destroy() {
        if (session != null) session.close();
        if (settings != null) settings.save();
        super.destroy();
    }

    // ------------------------------------------------------------------
    // Zugriff für Bildschirme
    // ------------------------------------------------------------------

    /** @return Benutzereinstellungen */
    public UserSettings getSettings() {
        return settings;
    }

    /** @return Oberflächenfabrik */
    public Ui getUi() {
        return ui;
    }

    /** @return Animationen */
    public Animator getAnimator() {
        return animator;
    }

    /** @return Sounds */
    public SoundManager getSounds() {
        return sounds;
    }

    /** @return Kamerasteuerung */
    public CameraRig getRig() {
        return rig;
    }

    /** @return 3D-Szene */
    public TableScene getTable() {
        return table;
    }

    /** @return Kartenfabrik */
    public CardFactory getCards() {
        return cards;
    }

    // ------------------------------------------------------------------
    // Bildschirme
    // ------------------------------------------------------------------

    private void show(Screen next) {
        closeOverlay();
        if (screen != null) stateManager.detach(screen);
        screen = next;
        if (game == null) backdrop.setEnabled(true);
        if (next != null) stateManager.attach(next);
    }

    private void showOverlay(Screen next) {
        closeOverlay();
        overlay = next;
        stateManager.attach(next);
    }

    private void closeOverlay() {
        if (overlay != null) stateManager.detach(overlay);
        overlay = null;
    }

    /**
     * Esc: schließt Overlays, öffnet im Spiel das Pausenmenü oder geht im Menü zurück.
     */
    private void onEscape() {
        if (overlay != null) overlay.onBack();
        else if (Dialog.dismissTop()) {
            // oberster Dialog geschlossen (oder nicht schließbar)
        }
        else if (game != null) game.onEscape();
        else if (screen != null) screen.onBack();
    }

    /**
     * @return laufende Spielansicht oder {@code null} (für Entwicklerwerkzeuge)
     */
    public GameScreen getGame() {
        return game;
    }

    /**
     * @return aktueller Menübildschirm oder {@code null}
     */
    public Screen getScreen() {
        return screen;
    }

    /**
     * @return Overlay über dem Spiel oder {@code null}
     */
    public Screen getOverlay() {
        return overlay;
    }

    /**
     * @return {@code true}, solange ein Overlay (Einstellungen, Regeln) über dem Spiel liegt
     */
    public boolean hasOverlay() {
        return overlay != null;
    }

    /**
     * Zeigt das Hauptmenü.
     */
    public void showMainMenu() {
        show(new MainMenuScreen());
    }

    /** Zeigt die Einstellungen für ein Solospiel. */
    public void showSolo() {
        show(new SoloScreen());
    }

    /** Zeigt den Bildschirm zum Erstellen eines Netzwerkspiels. */
    public void showHost() {
        show(new HostScreen());
    }

    /** Zeigt den Bildschirm zum Beitreten. */
    public void showJoin() {
        show(new JoinScreen());
    }

    /**
     * @param inGame {@code true}, wenn die Einstellungen über dem Spiel liegen
     */
    public void showSettings(boolean inGame) {
        if (inGame) showOverlay(new SettingsScreen(true, this::closeOverlay));
        else show(new SettingsScreen(false, this::showMainMenu));
    }

    /**
     * @param inGame {@code true}, wenn die Regeln über dem Spiel liegen
     */
    public void showHelp(boolean inGame) {
        if (inGame || game != null) showOverlay(new HelpScreen(true, this::closeOverlay));
        else show(new HelpScreen(false, this::showMainMenu));
    }

    /**
     * Zeigt eine Fehlermeldung über dem Hauptmenü.
     *
     * @param title Überschrift
     * @param text  Text
     */
    public void showError(String title, String text) {
        leaveSession();
        showMainMenu();
        new Dialog(ui, this, title, text).addButton("OK", ButtonVariant.PRIMARY, null).show();
    }

    // ------------------------------------------------------------------
    // Sitzungen
    // ------------------------------------------------------------------

    /**
     * Startet ein Solospiel mit eigenem Server und Bots.
     *
     * @param playerName Spielername
     */
    public void startSolo(String playerName) {
        leaveSession();
        Session s = new Session(this, Session.Mode.SOLO, playerName);
        try {
            s.startServer(0, settings.getBotSpeed());
        }
        catch (IOException e) {
            showError("Solospiel nicht möglich", "Es konnte kein lokaler Server gestartet werden: " + e.getMessage());
            return;
        }
        session = s;
        show(new LobbyScreen(s));
        s.connectAsync("localhost", s.port(), error -> showError("Solospiel nicht möglich", error));
    }

    /**
     * Startet ein neues Solospiel mit denselben Einstellungen.
     */
    public void restartSolo() {
        startSolo(settings.getPlayerName());
    }

    /**
     * Eröffnet ein Netzwerkspiel.
     *
     * @param playerName Spielername
     * @param port       Port
     * @return Fehlermeldung oder {@code null}
     */
    public String startHost(String playerName, int port) {
        leaveSession();
        Session s = new Session(this, Session.Mode.HOST, playerName);
        try {
            s.startServer(port, settings.getBotSpeed());
        }
        catch (IOException e) {
            return "Port " + port + " ist belegt oder gesperrt.";
        }
        session = s;
        show(new LobbyScreen(s));
        s.connectAsync("localhost", port, error -> showError("Verbindung fehlgeschlagen", error));
        return null;
    }

    /**
     * Tritt einem Netzwerkspiel bei. Die Lobby erscheint, sobald die Verbindung steht.
     *
     * @param playerName Spielername
     * @param host       Server
     * @param port       Port
     * @param onError    Fehlermeldung an den Beitrittsbildschirm
     */
    public void startJoin(String playerName, String host, int port, Consumer<String> onError) {
        leaveSession();
        Session s = new Session(this, Session.Mode.JOIN, playerName);
        session = s;
        s.logic().getEvents().addListener(new GameEventListener() {
            @Override
            public void onNameConfirmed(NameConfirmedEvent event) {
                // Erst nach der Begrüßung in die Lobby: Ein abgewiesener Client (Spiel voll oder
                // läuft bereits) wird zwar kurz verbunden, bekommt aber keine Begrüßung und bleibt
                // mit der Meldung auf dem Beitreten-Bildschirm.
                s.logic().getEvents().removeListener(this);
                if (session == s) show(new LobbyScreen(s));
            }

            @Override
            public void onDisconnected(DisconnectedEvent event) {
                // Abgewiesen, bevor die Lobby erschien (falsche Version, Spiel voll …)
                s.logic().getEvents().removeListener(this);
                if (session != s) return;
                session = null;
                s.close();
                onError.accept(joinFailure(event.reason(), host, port));
            }
        });
        s.connectAsync(host, port, error -> {
            if (session == s) session = null;
            s.close();
            onError.accept(error);
        });
    }

    /**
     * Übersetzt den Grund einer Abweisung beim Beitreten in eine verständliche Meldung.
     */
    private static String joinFailure(String reason, String host, int port) {
        if (reason != null && reason.contains("mismatch"))
            return "Der Server unter " + host + ":" + port + " nutzt eine andere Version von WinfToSlay.";
        if (reason == null || reason.startsWith("Die Verbindung zum Server"))
            return "Der Server unter " + host + ":" + port + " hat die Verbindung beendet. "
                   + "Läuft dort WinfToSlay in derselben Version?";
        return reason;
    }

    /**
     * Wechselt von der Lobby ins Spiel.
     *
     * @param s     Sitzung
     * @param event Startereignis
     */
    public void startGame(Session s, GameStartedEvent event) {
        if (s != session) return;
        show(null);
        backdrop.setEnabled(false);
        game = new GameScreen(this, s);
        stateManager.attach(game);
        game.start(event);
    }

    /**
     * Startet die Effekt-Vorführung (nur für Entwickler).
     *
     * @param s     Sitzung ohne Verbindung
     * @param event erfundenes Startereignis
     */
    public void startShowcase(Session s, GameStartedEvent event) {
        leaveSession();
        session = s;
        startGame(s, event);
    }

    /**
     * Beendet die aktuelle Sitzung (Spiel verlassen, Server stoppen).
     */
    public void leaveSession() {
        Dialog.closeAll();
        closeOverlay();
        if (game != null) {
            game.dispose();
            stateManager.detach(game);
            game = null;
        }
        if (session != null) {
            session.close();
            session = null;
        }
        animator.clear();
    }

    /**
     * @param state Zustand
     * @return {@code true}, wenn er angehängt ist
     */
    public boolean isAttached(AppState state) {
        return stateManager.hasState(state);
    }
}
