package pp.winf2slay.controller.message.client;

/**
 * Visitor für alle {@link ClientMessage}s. Implementiert vom Server.
 *
 * <p>Alle Methoden haben eine leere Standardimplementierung; Implementierungen
 * überschreiben nur die Nachrichten, die sie tatsächlich verarbeiten.</p>
 */
public interface ClientMessageInterpreter {

    /** @param msg Nachricht @param from Absender */
    default void received(CMJoinLobby msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMLeaveLobby msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMAddBot msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMRemoveBot msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMStartGame msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMDrawCard msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMPlayCard msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMActivateHeroEffect msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMAttackMonster msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMMulligan msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMEndTurn msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMChallengeResponse msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMModifySingleResponse msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMModifyDoubleResponse msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMHeroSelectionResponse msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMPlayerSelectionResponse msg, int from) {}

    /** @param msg Nachricht @param from Absender */
    default void received(CMAnimationsDone msg, int from) {}
}
