package pp.winf2slay.view.ui;

/**
 * Farbvarianten der Schaltflächen.
 */
public enum ButtonVariant {
    /** Ziegelrot – Hauptaktion. */
    PRIMARY("primary"),
    /** Dunkles Holz – Nebenaktion. */
    SECONDARY("secondary"),
    /** Grün – Bestätigen/Starten. */
    SUCCESS("success"),
    /** Gold – besondere Hervorhebung. */
    GOLD("gold");

    private final String texture;

    ButtonVariant(String texture) {
        this.texture = texture;
    }

    /**
     * @return Namensteil der Textur
     */
    public String getTexture() {
        return texture;
    }
}
