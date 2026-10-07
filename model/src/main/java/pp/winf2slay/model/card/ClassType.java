package pp.winf2slay.model.card;

/**
 * Heldenklassen. Wer Anführer und Helden aller sechs Klassen in seiner Gruppe
 * vereint, gewinnt das Spiel.
 */
public enum ClassType {
    MAGE("Magier"),
    GUARD("Wächter"),
    FIGHTER("Kämpfer"),
    RANGER("Waldläufer"),
    THIEF("Dieb"),
    BARD("Barde");

    private final String displayName;

    ClassType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return deutscher Klassenname, wie er auf den Karten steht
     */
    public String getDisplayName() {
        return displayName;
    }
}
