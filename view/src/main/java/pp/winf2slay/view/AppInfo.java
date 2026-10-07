package pp.winf2slay.view;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Programmversion. Sie steht nur in {@code gradle.properties} ({@code appVersion}); Gradle
 * trägt sie beim Bauen in {@code version.properties} ein (Hauptmenü, Protokoll, Release).
 */
public final class AppInfo {

    /**
     * Version, z. B. {@code 2.0.1}; {@code dev}, wenn ohne Gradle gebaut wurde.
     */
    public static final String VERSION = loadVersion();

    private AppInfo() {
    }

    private static String loadVersion() {
        try (InputStream in = AppInfo.class.getResourceAsStream("/version.properties")) {
            if (in == null) return "dev";
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty("version", "").trim();
            // ohne Gradle (z. B. reiner IntelliJ-Build) bleibt der Platzhalter stehen
            return v.isEmpty() || v.contains("$") ? "dev" : v;
        }
        catch (IOException e) {
            return "dev";
        }
    }
}
