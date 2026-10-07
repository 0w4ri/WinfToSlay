package pp.winf2slay.controller.message.server;

/**
 * Visitor für alle {@link ServerMessage}s. Implementiert von Client-Zuständen
 * und Bots.
 *
 * <p>Alle Methoden haben eine leere Standardimplementierung; Implementierungen
 * überschreiben nur die Nachrichten, die sie tatsächlich verarbeiten.</p>
 */
public interface ServerMessageInterpreter {

    // --- direkte Nachrichten ---

    /** @param msg Nachricht */
    default void received(SMWelcome msg) {}

    /** @param msg Nachricht */
    default void received(SMLobbyUpdate msg) {}

    /** @param msg Nachricht */
    default void received(SMTurnSwitch msg) {}

    /** @param msg Nachricht */
    default void received(SMContinueTurn msg) {}

    /** @param msg Nachricht */
    default void received(SMActionRejected msg) {}

    /** @param msg Nachricht */
    default void received(SMChallengeRequest msg) {}

    /** @param msg Nachricht */
    default void received(SMModifySingleRequest msg) {}

    /** @param msg Nachricht */
    default void received(SMModifyDoubleRequest msg) {}

    /** @param msg Nachricht */
    default void received(SMHeroSelectionRequest msg) {}

    /** @param msg Nachricht */
    default void received(SMPlayerSelectionRequest msg) {}

    /** @param msg Nachricht */
    default void received(SMNotice msg) {}

    /** @param msg Nachricht */
    default void received(SMGameOver msg) {}

    // --- Broadcasts (müssen bestätigt werden) ---

    /** @param msg Nachricht */
    default void received(BCGameStarted msg) {}

    /** @param msg Nachricht */
    default void received(BCCardMoved msg) {}

    /** @param msg Nachricht */
    default void received(BCCardsMoved msg) {}

    /** @param msg Nachricht */
    default void received(BCMulligan msg) {}

    /** @param msg Nachricht */
    default void received(BCEffectActivated msg) {}

    /** @param msg Nachricht */
    default void received(BCMonsterAttacked msg) {}

    /** @param msg Nachricht */
    default void received(BCDiceRolled msg) {}

    /** @param msg Nachricht */
    default void received(BCDiceModified msg) {}

    /** @param msg Nachricht */
    default void received(BCRollResolved msg) {}

    /** @param msg Nachricht */
    default void received(BCChallengeRolled msg) {}

    /** @param msg Nachricht */
    default void received(BCChallengeModified msg) {}

    /** @param msg Nachricht */
    default void received(BCChallengeResolved msg) {}

    /** @param msg Nachricht */
    default void received(BCTurnEnded msg) {}
}
