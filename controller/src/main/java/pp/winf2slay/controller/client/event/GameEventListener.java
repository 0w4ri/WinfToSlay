package pp.winf2slay.controller.client.event;

/**
 * Empfänger von {@link GameEvent}s. Alle Methoden sind leer vorbelegt;
 * Implementierungen überschreiben nur die Ereignisse, die sie interessieren.
 */
public interface GameEventListener {

    // --- Verbindung & Lobby ---

    /** @param event Ereignis */
    default void onConnected(ConnectedEvent event) {}

    /** @param event Ereignis */
    default void onDisconnected(DisconnectedEvent event) {}

    /** @param event Ereignis */
    default void onNameConfirmed(NameConfirmedEvent event) {}

    /** @param event Ereignis */
    default void onLobbyChanged(LobbyChangedEvent event) {}

    /** @param event Ereignis */
    default void onNotice(NoticeEvent event) {}

    // --- Spielablauf ---

    /** @param event Ereignis */
    default void onGameStarted(GameStartedEvent event) {}

    /** @param event Ereignis */
    default void onGameOver(GameOverEvent event) {}

    /** @param event Ereignis */
    default void onTurnStarted(TurnStartedEvent event) {}

    /** @param event Ereignis */
    default void onTurnEnded(TurnEndedEvent event) {}

    /** @param event Ereignis */
    default void onActionStateChanged(ActionStateChangedEvent event) {}

    /** @param event Ereignis */
    default void onStateSynced(StateSyncedEvent event) {}

    // --- Animationen ---

    /** @param event Ereignis */
    default void onCardMoved(CardMovedEvent event) {}

    /** @param event Ereignis */
    default void onCardsMoved(CardsMovedEvent event) {}

    /** @param event Ereignis */
    default void onMulligan(MulliganEvent event) {}

    /** @param event Ereignis */
    default void onEffectActivated(EffectActivatedEvent event) {}

    /** @param event Ereignis */
    default void onMonsterAttacked(MonsterAttackedEvent event) {}

    /** @param event Ereignis */
    default void onDiceRolled(DiceRolledEvent event) {}

    /** @param event Ereignis */
    default void onDiceModified(DiceModifiedEvent event) {}

    /** @param event Ereignis */
    default void onRollResolved(RollResolvedEvent event) {}

    /** @param event Ereignis */
    default void onChallengeRolled(ChallengeRolledEvent event) {}

    /** @param event Ereignis */
    default void onChallengeModified(ChallengeModifiedEvent event) {}

    /** @param event Ereignis */
    default void onChallengeResolved(ChallengeResolvedEvent event) {}

    // --- Anfragen ---

    /** @param event Ereignis */
    default void onChallengeRequest(ChallengeRequestEvent event) {}

    /** @param event Ereignis */
    default void onModifySingleRequest(ModifySingleRequestEvent event) {}

    /** @param event Ereignis */
    default void onModifyDoubleRequest(ModifyDoubleRequestEvent event) {}

    /** @param event Ereignis */
    default void onHeroSelectionRequest(HeroSelectionRequestEvent event) {}

    /** @param event Ereignis */
    default void onPlayerSelectionRequest(PlayerSelectionRequestEvent event) {}

    /** @param event Ereignis */
    default void onRequestClosed(RequestClosedEvent event) {}
}
