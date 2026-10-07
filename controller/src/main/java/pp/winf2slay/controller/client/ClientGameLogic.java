package pp.winf2slay.controller.client;

import pp.winf2slay.controller.client.event.ActionStateChangedEvent;
import pp.winf2slay.controller.client.event.ConnectedEvent;
import pp.winf2slay.controller.client.event.DisconnectedEvent;
import pp.winf2slay.controller.client.event.GameEvent;
import pp.winf2slay.controller.client.event.GameEventBroker;
import pp.winf2slay.controller.client.event.RequestClosedEvent;
import pp.winf2slay.controller.client.event.StateSyncedEvent;
import pp.winf2slay.controller.client.state.ClientState;
import pp.winf2slay.controller.client.state.GameOverState;
import pp.winf2slay.controller.client.state.LobbyState;
import pp.winf2slay.controller.message.BotLevel;
import pp.winf2slay.controller.message.client.CMActivateHeroEffect;
import pp.winf2slay.controller.message.client.CMAddBot;
import pp.winf2slay.controller.message.client.CMAnimationsDone;
import pp.winf2slay.controller.message.client.CMAttackMonster;
import pp.winf2slay.controller.message.client.CMChallengeResponse;
import pp.winf2slay.controller.message.client.CMDrawCard;
import pp.winf2slay.controller.message.client.CMHeroSelectionResponse;
import pp.winf2slay.controller.message.client.CMJoinLobby;
import pp.winf2slay.controller.message.client.CMLeaveLobby;
import pp.winf2slay.controller.message.client.CMModifyDoubleResponse;
import pp.winf2slay.controller.message.client.CMModifySingleResponse;
import pp.winf2slay.controller.message.client.CMMulligan;
import pp.winf2slay.controller.message.client.CMPlayCard;
import pp.winf2slay.controller.message.client.CMPlayerSelectionResponse;
import pp.winf2slay.controller.message.client.CMRemoveBot;
import pp.winf2slay.controller.message.client.CMStartGame;
import pp.winf2slay.controller.message.client.ClientMessage;
import pp.winf2slay.controller.message.server.BroadcastMessage;
import pp.winf2slay.controller.message.server.SMChallengeRequest;
import pp.winf2slay.controller.message.server.SMHeroSelectionRequest;
import pp.winf2slay.controller.message.server.SMModifyDoubleRequest;
import pp.winf2slay.controller.message.server.SMModifySingleRequest;
import pp.winf2slay.controller.message.server.SMPlayerSelectionRequest;
import pp.winf2slay.controller.message.server.ServerMessage;
import pp.winf2slay.model.Action;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Challenge;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.card.Spell;
import pp.winf2slay.model.field.Group;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.Objects;

/**
 * Spiellogik eines Clients.
 *
 * <p>Die Klasse ist die einzige Schnittstelle der Oberfläche zum Spiel:</p>
 * <ul>
 *     <li>Eingehende Servernachrichten verarbeitet der aktuelle
 *     {@link ClientState}; daraus entstehen {@link GameEvent}s für die Oberfläche.</li>
 *     <li>Die Oberfläche löst Aktionen über die öffentlichen Methoden aus
 *     (z. B. {@link #drawCard()}); vorher kann sie mit {@code can…}-Methoden
 *     prüfen, ob die Aktion gerade erlaubt ist.</li>
 *     <li>Broadcasts werden erst bestätigt, wenn das {@link AnimationGate} der
 *     Oberfläche die zugehörigen Animationen abgeschlossen hat.</li>
 * </ul>
 * <p>Die Klasse ist nicht threadsicher; alle Aufrufe müssen aus demselben
 * Thread kommen (in der Anwendung: dem jME-Render-Thread).</p>
 */
public class ClientGameLogic {

    private static final Logger LOGGER = System.getLogger(ClientGameLogic.class.getName());

    private final ClientSender sender;
    private final GameEventBroker events;
    private final ClientGameState model = new ClientGameState();
    private AnimationGate animationGate = AnimationGate.IMMEDIATE;
    private ClientState state;
    private ServerMessage pendingRequest;

    /**
     * @param sender Versand an den Server
     * @param events Verteilung der Ereignisse an die Oberfläche
     */
    public ClientGameLogic(ClientSender sender, GameEventBroker events) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.events = Objects.requireNonNull(events, "events");
        setState(new LobbyState(this));
    }

    /**
     * @param sender Versand an den Server
     */
    public ClientGameLogic(ClientSender sender) {
        this(sender, new GameEventBroker());
    }

    // ------------------------------------------------------------------
    // Infrastruktur
    // ------------------------------------------------------------------

    /**
     * @return Ereignisverteilung
     */
    public GameEventBroker getEvents() {
        return events;
    }

    /**
     * @return lokaler Spielzustand
     */
    public ClientGameState getModel() {
        return model;
    }

    /**
     * @return aktueller Zustand
     */
    public ClientState getState() {
        return state;
    }

    /**
     * Wechselt den Zustand.
     *
     * @param newState neuer Zustand
     */
    public void setState(ClientState newState) {
        LOGGER.log(Level.DEBUG, "Client-Zustand {0} --> {1}", state, newState);
        state = newState;
        state.entry();
        events.notifyListeners(new ActionStateChangedEvent(state.canAct(), model.getActionPoints()));
    }

    /**
     * Legt fest, wann Broadcasts bestätigt werden.
     *
     * @param animationGate Gate der Oberfläche
     */
    public void setAnimationGate(AnimationGate animationGate) {
        this.animationGate = Objects.requireNonNull(animationGate);
    }

    /**
     * Verteilt ein Ereignis an die Oberfläche.
     *
     * @param event Ereignis
     */
    public void notify(GameEvent event) {
        events.notifyListeners(event);
    }

    /**
     * Sendet eine Nachricht an den Server.
     *
     * @param msg Nachricht
     */
    public void send(ClientMessage msg) {
        LOGGER.log(Level.DEBUG, "sende {0}", msg);
        sender.send(msg);
    }

    /**
     * Verarbeitet eine Nachricht des Servers.
     *
     * @param msg Nachricht
     */
    public void receive(ServerMessage msg) {
        LOGGER.log(Level.DEBUG, "empfangen {0}", msg);
        if (msg instanceof BroadcastMessage broadcast) {
            model.apply(broadcast.getSync());
            notify(new StateSyncedEvent());
            msg.accept(state);
            int syncId = broadcast.getSyncId();
            animationGate.afterAnimations(() -> send(new CMAnimationsDone(syncId)));
        }
        else {
            msg.accept(state);
        }
    }

    /**
     * Die Netzwerkverbindung wurde aufgebaut.
     */
    public void connected() {
        notify(new ConnectedEvent());
    }

    /**
     * Die Netzwerkverbindung wurde getrennt.
     *
     * @param reason Begründung
     */
    public void disconnected(String reason) {
        if (!(state instanceof GameOverState)) {
            setState(new GameOverState(this));
            notify(new DisconnectedEvent(reason));
        }
    }

    // ------------------------------------------------------------------
    // Anfragen des Servers
    // ------------------------------------------------------------------

    /**
     * Merkt sich eine offene Anfrage des Servers.
     *
     * @param request Anfrage
     */
    public void setPendingRequest(ServerMessage request) {
        this.pendingRequest = request;
    }

    /**
     * @return offene Anfrage oder {@code null}
     */
    public ServerMessage getPendingRequest() {
        return pendingRequest;
    }

    private boolean answer(Class<? extends ServerMessage> requestType, ClientMessage response) {
        if (!requestType.isInstance(pendingRequest)) {
            LOGGER.log(Level.WARNING, "Keine passende Anfrage für {0}", response);
            return false;
        }
        pendingRequest = null;
        send(response);
        notify(new RequestClosedEvent());
        return true;
    }

    /**
     * Antwortet auf eine Herausforderungsanfrage.
     *
     * @param challenge {@code true}, um herauszufordern
     */
    public void respondToChallenge(boolean challenge) {
        Challenge card = challenge
                         ? model.getHand().stream().filter(Challenge.class::isInstance).map(Challenge.class::cast)
                                .findFirst().orElse(null)
                         : null;
        answer(SMChallengeRequest.class, new CMChallengeResponse(card));
    }

    /**
     * Antwortet auf die Frage, ob ein einzelner Wurf modifiziert wird.
     *
     * @param modification gespielte Modifikation oder {@code null}
     */
    public void respondToModification(Modification modification) {
        answer(SMModifySingleRequest.class,
               new CMModifySingleResponse(modification));
    }

    /**
     * Antwortet auf die Frage, ob ein Wurf einer Herausforderung modifiziert wird.
     *
     * @param modification gespielte Modifikation oder {@code null}
     * @param targetPlayer Spieler, dessen Wurf verändert wird
     */
    public void respondToChallengeModification(Modification modification, String targetPlayer) {
        answer(SMModifyDoubleRequest.class,
               new CMModifyDoubleResponse(modification, modification == null ? null : targetPlayer));
    }

    /**
     * Wählt einen Helden als Ziel.
     *
     * @param hero gewählter Held
     */
    public void chooseHero(Hero hero) {
        answer(SMHeroSelectionRequest.class, new CMHeroSelectionResponse(hero));
    }

    /**
     * Wählt einen Spieler als Ziel.
     *
     * @param playerName gewählter Spieler
     */
    public void choosePlayer(String playerName) {
        answer(SMPlayerSelectionRequest.class,
               new CMPlayerSelectionResponse(playerName));
    }

    // ------------------------------------------------------------------
    // Lobby
    // ------------------------------------------------------------------

    /**
     * Meldet den Spieler mit seinem Wunschnamen an.
     *
     * @param name Wunschname
     */
    public void joinLobby(String name) {
        send(new CMJoinLobby(name));
    }

    /**
     * Verlässt die Lobby.
     */
    public void leaveLobby() {
        send(new CMLeaveLobby());
    }

    /**
     * Fügt einen Bot hinzu (nur Spielleiter).
     *
     * @param level Spielstärke
     */
    public void addBot(BotLevel level) {
        if (model.isHost()) send(new CMAddBot(level));
    }

    /**
     * Entfernt einen Bot (nur Spielleiter).
     *
     * @param name Name des Bots
     */
    public void removeBot(String name) {
        if (model.isHost()) send(new CMRemoveBot(name));
    }

    /**
     * Startet die Partie (nur Spielleiter).
     */
    public void startGame() {
        if (model.isHost()) send(new CMStartGame());
    }

    // ------------------------------------------------------------------
    // Aktionen im eigenen Zug
    // ------------------------------------------------------------------

    /**
     * @return {@code true}, wenn dieser Client gerade Aktionen ausführen darf
     */
    public boolean canAct() {
        return state.canAct();
    }

    /**
     * @param action Aktion
     * @return {@code true}, wenn genug Aktionspunkte vorhanden sind und Aktionen erlaubt sind
     */
    public boolean canAfford(Action action) {
        return canAct() && model.getActionPoints() >= action.getCost();
    }

    /**
     * @return {@code true}, wenn eine Karte gezogen werden darf
     */
    public boolean canDraw() {
        return canAfford(Action.DRAW_CARD) && model.getSupportCount() + model.getDiscardCount() > 0;
    }

    /**
     * @param card Handkarte
     * @return {@code true}, wenn die Karte ausgespielt werden darf
     */
    public boolean canPlay(Card card) {
        if (!canAfford(Action.PLAY_CARD) || !model.getHand().contains(card)) return false;
        if (card instanceof Spell) return true;
        return card instanceof Hero
               && model.getMe().map(me -> me.getHeroCount() < Group.SIZE).orElse(false);
    }

    /**
     * @param hero Held der eigenen Gruppe
     * @return {@code true}, wenn sein Effekt ausgelöst werden darf
     */
    public boolean canActivate(Hero hero) {
        if (!canAfford(Action.ACTIVATE_HERO) || model.isHeroUsed(hero)) return false;
        return model.getMe().map(me -> Arrays.asList(me.getGroup()).contains(hero)).orElse(false);
    }

    /**
     * @param monster offenes Monster
     * @return {@code true}, wenn das Monster angegriffen werden darf
     */
    public boolean canAttack(Monster monster) {
        return canAfford(Action.ATTACK_MONSTER)
               && Arrays.asList(model.getOpenMonsters()).contains(monster)
               && model.getMe().map(me -> me.getHeroCount() >= Action.HEROES_TO_ATTACK).orElse(false);
    }

    /**
     * @return {@code true}, wenn ein Mulligan erlaubt ist
     */
    public boolean canMulligan() {
        return canAfford(Action.MULLIGAN);
    }

    /**
     * Zieht eine Karte.
     */
    public void drawCard() {
        if (canDraw()) state.perform(Action.DRAW_CARD, new CMDrawCard());
    }

    /**
     * Spielt einen Helden oder Zauber aus.
     *
     * @param card Handkarte
     */
    public void playCard(Card card) {
        if (!canPlay(card)) return;
        if (card instanceof Hero hero) model.markHeroUsed(hero);
        state.perform(Action.PLAY_CARD, new CMPlayCard(card));
    }

    /**
     * Löst den Effekt eines eigenen Helden aus.
     *
     * @param hero Held
     */
    public void activateHero(Hero hero) {
        if (!canActivate(hero)) return;
        model.markHeroUsed(hero);
        state.perform(Action.ACTIVATE_HERO, new CMActivateHeroEffect(hero));
    }

    /**
     * Greift ein Monster an.
     *
     * @param monster Monster
     */
    public void attackMonster(Monster monster) {
        if (canAttack(monster)) state.perform(Action.ATTACK_MONSTER, new CMAttackMonster(monster));
    }

    /**
     * Führt einen Mulligan durch.
     */
    public void mulligan() {
        if (canMulligan()) state.perform(Action.MULLIGAN, new CMMulligan());
    }

    /**
     * Beendet den eigenen Zug.
     */
    public void endTurn() {
        if (canAct()) state.endTurn();
    }
}
