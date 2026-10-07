package pp.winf2slay.view.settings;

import pp.winf2slay.controller.message.BotLevel;
import pp.winf2slay.controller.network.WinfToSlayServer;
import pp.winf2slay.controller.server.state.LobbyState;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Dauerhafte Benutzereinstellungen (über {@link Preferences} gespeichert).
 *
 * <p>Änderungen gelten sofort (Beobachter werden benachrichtigt) und werden mit
 * {@link #save()} dauerhaft gespeichert.</p>
 */
public class UserSettings {

    /** Höchstlänge des Spielernamens im Eingabefeld. */
    public static final int MAX_NAME = 14;

    /**
     * Grafikqualität.
     */
    public enum Quality {
        /**
         * Schatten, Leuchten, Kantenglättung und viele Partikel.
         */
        HIGH("Hoch"),
        /**
         * Für schwächere Rechner: ohne Schatten und Nachbearbeitung.
         */
        LOW("Niedrig");

        private final String displayName;

        Quality(String displayName) {
            this.displayName = displayName;
        }

        /**
         * @return deutscher Anzeigename
         */
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Vorgaben für das Animationstempo.
     */
    public enum AnimationSpeed {
        /**
         * Gemütlich.
         */
        SLOW("Gemütlich", 0.75f),
        /**
         * Standard.
         */
        NORMAL("Normal", 1f),
        /**
         * Zügig.
         */
        FAST("Schnell", 1.5f),
        /**
         * Für Erfahrene.
         */
        TURBO("Turbo", 2.5f);

        private final String displayName;
        private final float factor;

        AnimationSpeed(String displayName, float factor) {
            this.displayName = displayName;
            this.factor = factor;
        }

        /**
         * @return deutscher Anzeigename
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * @return Tempofaktor
         */
        public float getFactor() {
            return factor;
        }
    }

    private static final String KEY_NAME = "playerName";
    private static final String KEY_HOST = "lastHost";
    private static final String KEY_PORT = "lastPort";
    private static final String KEY_MUSIC = "musicVolume";
    private static final String KEY_EFFECTS = "effectsVolume";
    private static final String KEY_ANIMATION = "animationSpeed";
    private static final String KEY_BOT_SPEED = "botSpeed";
    private static final String KEY_SOLO_BOTS = "soloBots";
    private static final String KEY_SOLO_LEVEL = "soloLevel";
    private static final String KEY_QUALITY = "quality";
    private static final String KEY_SHAKE = "cameraShake";
    private static final String KEY_FPS = "showFps";
    private static final String KEY_TIPS = "showTips";

    private final Preferences prefs;
    private final List<Runnable> listeners = new ArrayList<>();
    private boolean dirty;

    /**
     * Lädt die Einstellungen des aktuellen Benutzers.
     */
    public UserSettings() {
        this(Preferences.userNodeForPackage(UserSettings.class));
    }

    /**
     * @param prefs Speicherort (für Tests austauschbar)
     */
    public UserSettings(Preferences prefs) {
        this.prefs = prefs;
    }

    /**
     * Registriert einen Beobachter, der nach jeder Änderung aufgerufen wird.
     *
     * @param listener Beobachter
     */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void changed() {
        dirty = true;
        listeners.forEach(Runnable::run);
    }

    /**
     * Schreibt geänderte Einstellungen dauerhaft weg. Wird regelmäßig und beim
     * Beenden aufgerufen, damit z. B. Schieberegler nicht bei jeder Bewegung speichern.
     */
    public void save() {
        if (!dirty) return;
        dirty = false;
        try {
            prefs.flush();
        }
        catch (BackingStoreException e) {
            System.getLogger(UserSettings.class.getName()).log(System.Logger.Level.WARNING,
                                                                "Einstellungen konnten nicht gespeichert werden", e);
        }
    }

    /**
     * @return zuletzt verwendeter Spielername
     */
    public String getPlayerName() {
        String name = LobbyState.sanitize(prefs.get(KEY_NAME, defaultName()), "Spieler");
        return name.length() > MAX_NAME ? name.substring(0, MAX_NAME).strip() : name;
    }

    private static String defaultName() {
        String user = LobbyState.sanitize(System.getProperty("user.name", ""), "");
        if (user.isBlank() || user.length() > MAX_NAME) return "Spieler";
        return Character.toUpperCase(user.charAt(0)) + user.substring(1);
    }

    /**
     * @param name Spielername
     */
    public void setPlayerName(String name) {
        prefs.put(KEY_NAME, name);
        changed();
    }

    /**
     * @return zuletzt verwendeter Server
     */
    public String getLastHost() {
        return prefs.get(KEY_HOST, "localhost");
    }

    /**
     * @param host Server
     */
    public void setLastHost(String host) {
        prefs.put(KEY_HOST, host);
        changed();
    }

    /**
     * @return zuletzt verwendeter Port
     */
    public int getLastPort() {
        return prefs.getInt(KEY_PORT, WinfToSlayServer.DEFAULT_PORT);
    }

    /**
     * @param port Port
     */
    public void setLastPort(int port) {
        prefs.putInt(KEY_PORT, port);
        changed();
    }

    /**
     * @return Musiklautstärke 0..1
     */
    public float getMusicVolume() {
        return prefs.getFloat(KEY_MUSIC, 0.35f);
    }

    /**
     * @param volume Musiklautstärke 0..1
     */
    public void setMusicVolume(float volume) {
        prefs.putFloat(KEY_MUSIC, clamp(volume));
        changed();
    }

    /**
     * @return Effektlautstärke 0..1
     */
    public float getEffectsVolume() {
        return prefs.getFloat(KEY_EFFECTS, 0.7f);
    }

    /**
     * @param volume Effektlautstärke 0..1
     */
    public void setEffectsVolume(float volume) {
        prefs.putFloat(KEY_EFFECTS, clamp(volume));
        changed();
    }

    /**
     * @return Animationstempo
     */
    public AnimationSpeed getAnimationSpeed() {
        return parse(AnimationSpeed.class, prefs.get(KEY_ANIMATION, null), AnimationSpeed.NORMAL);
    }

    /**
     * @param speed Animationstempo
     */
    public void setAnimationSpeed(AnimationSpeed speed) {
        prefs.put(KEY_ANIMATION, speed.name());
        changed();
    }

    /**
     * Bedenkzeit der Bots als Faktor (1 = normal, kleiner = schneller).
     *
     * @return Faktor
     */
    public float getBotSpeed() {
        return prefs.getFloat(KEY_BOT_SPEED, 1f);
    }

    /**
     * @param factor Bedenkzeit-Faktor
     */
    public void setBotSpeed(float factor) {
        prefs.putFloat(KEY_BOT_SPEED, Math.max(0.1f, Math.min(3f, factor)));
        changed();
    }

    /**
     * @return Anzahl der Bots im Solospiel
     */
    public int getSoloBots() {
        return Math.max(1, Math.min(5, prefs.getInt(KEY_SOLO_BOTS, 2)));
    }

    /**
     * @param count Anzahl der Bots im Solospiel (1–5)
     */
    public void setSoloBots(int count) {
        prefs.putInt(KEY_SOLO_BOTS, Math.max(1, Math.min(5, count)));
        changed();
    }

    /**
     * @return Schwierigkeit der Bots im Solospiel; {@code null} bedeutet „gemischt“
     */
    public BotLevel getSoloLevel() {
        String value = prefs.get(KEY_SOLO_LEVEL, BotLevel.NORMAL.name());
        return "MIXED".equals(value) ? null : parse(BotLevel.class, value, BotLevel.NORMAL);
    }

    /**
     * @param level Schwierigkeit; {@code null} für gemischt
     */
    public void setSoloLevel(BotLevel level) {
        prefs.put(KEY_SOLO_LEVEL, level == null ? "MIXED" : level.name());
        changed();
    }

    /**
     * @return Grafikqualität
     */
    public Quality getQuality() {
        return parse(Quality.class, prefs.get(KEY_QUALITY, null), Quality.HIGH);
    }

    /**
     * @param quality Grafikqualität
     */
    public void setQuality(Quality quality) {
        prefs.put(KEY_QUALITY, quality.name());
        changed();
    }

    /**
     * @return {@code true}, wenn die Kamera bei Explosionen wackeln darf
     */
    public boolean isCameraShake() {
        return prefs.getBoolean(KEY_SHAKE, true);
    }

    /**
     * @param shake Kamerawackeln an/aus
     */
    public void setCameraShake(boolean shake) {
        prefs.putBoolean(KEY_SHAKE, shake);
        changed();
    }

    /**
     * @return {@code true}, wenn die Bildrate angezeigt wird
     */
    public boolean isShowFps() {
        return prefs.getBoolean(KEY_FPS, false);
    }

    /**
     * @param show Bildrate anzeigen
     */
    public void setShowFps(boolean show) {
        prefs.putBoolean(KEY_FPS, show);
        changed();
    }

    /**
     * @return {@code true}, wenn Spieltipps eingeblendet werden
     */
    public boolean isShowTips() {
        return prefs.getBoolean(KEY_TIPS, true);
    }

    /**
     * @param show Spieltipps anzeigen
     */
    public void setShowTips(boolean show) {
        prefs.putBoolean(KEY_TIPS, show);
        changed();
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, E fallback) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, value);
        }
        catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
