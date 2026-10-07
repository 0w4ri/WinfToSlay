package pp.winf2slay.controller.message;

/**
 * Spielstärke eines Bots.
 */
public enum BotLevel {
    /** Spielt zufällige, aber gültige Züge und reagiert selten auf Gegner. */
    EASY("Leicht"),
    /** Bewertet Züge nach einfachen Heuristiken. */
    NORMAL("Normal"),
    /** Wie Normal, verfolgt aber gezielt die Siegbedingungen und stört führende Gegner. */
    HARD("Schwer");

    private final String displayName;

    BotLevel(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return deutscher Anzeigename
     */
    public String getDisplayName() {
        return displayName;
    }
}
