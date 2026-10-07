package pp.winf2slay.controller.message;

/**
 * Orte, an denen eine Karte liegen kann. Wird in Bewegungsnachrichten verwendet,
 * damit die Oberfläche die passende Animation wählen kann.
 */
public enum CardPosition {
    /** Handkarten eines Spielers */
    PLAYER_HAND,
    /** Aktionsfeld („Hover“) eines Spielers */
    PLAYER_HOVER,
    /** Heldengruppe eines Spielers */
    PLAYER_GROUP,
    /** Besiegte Monster eines Spielers */
    PLAYER_MONSTERS,
    /** Unterstützungsstapel (Nachziehstapel) */
    SUPPORT_DECK,
    /** Ablagestapel */
    DISCARD_PILE,
    /** Offen ausliegende Monster in der Tischmitte */
    OPEN_MONSTERS
}
