package pp.winf2slay.controller.server;

import pp.winf2slay.controller.message.BotLevel;
import pp.winf2slay.controller.message.LobbyEntry;
import pp.winf2slay.controller.message.client.CMAnimationsDone;
import pp.winf2slay.controller.message.client.ClientMessage;
import pp.winf2slay.controller.message.server.BroadcastMessage;
import pp.winf2slay.controller.message.server.ClientSync;
import pp.winf2slay.controller.message.server.PlayerSnapshot;
import pp.winf2slay.controller.message.server.SMLobbyUpdate;
import pp.winf2slay.controller.message.server.SMNotice;
import pp.winf2slay.controller.message.server.ServerMessage;
import pp.winf2slay.controller.server.bot.BotManager;
import pp.winf2slay.controller.server.state.LobbyState;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Card;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Zentrale Spiellogik des Servers.
 *
 * <p>Verwaltet das {@link Game}, die Zustandsmaschine ({@link ServerState}),
 * den aktiven Spieler und die Bots. Zwei Mechanismen steuern den Ablauf:</p>
 * <ul>
 *     <li><b>Broadcasts</b> ({@link #broadcast(BroadcastMessage, Runnable)}): Ein
 *     Spielereignis wird an alle Spieler gesendet; die Fortsetzung läuft erst,
 *     wenn jeder Spieler die Animationen bestätigt hat.</li>
 *     <li><b>Anfragen</b> ({@link #request(Player, ServerMessage, Class, Consumer)}):
 *     Ein Spieler muss eine Entscheidung treffen (z. B. Herausforderung ja/nein).</li>
 * </ul>
 * <p>Solange ein Broadcast oder eine Anfrage offen ist, werden andere
 * Aktionen ignoriert. Alle Methoden laufen im Server-Thread.</p>
 */
public class ServerGameLogic {

    static final Logger LOGGER = System.getLogger(ServerGameLogic.class.getName());

    /**
     * Spätestens nach dieser Zeit wird ein Broadcast als bestätigt behandelt,
     * damit ein hängender Client das Spiel nicht blockiert.
     */
    public static final long SYNC_TIMEOUT_MILLIS = 30_000;

    private final Game game;
    private final ServerSender sender;
    private final ServerScheduler scheduler;
    private final BotManager bots;
    private final TurnInfo turn = new TurnInfo();

    private ServerState state;
    private int activeIndex;
    private int hostId = Integer.MIN_VALUE;
    private int nextSyncId = 1;
    private int nextBotId = -1;
    private PendingSync pendingSync;
    private PendingRequest<?> pendingRequest;
    private Runnable onGameFinished = () -> {};

    /**
     * Erzeugt die Serverlogik mit einem neuen, zufällig gemischten Spiel.
     *
     * @param sender    Versand an menschliche Spieler
     * @param scheduler Ausführung zeitversetzter Aufgaben im Server-Thread
     */
    public ServerGameLogic(ServerSender sender, ServerScheduler scheduler) {
        this(sender, scheduler, new Game());
    }

    /**
     * Erzeugt die Serverlogik für ein vorgegebenes Spiel (z. B. mit festem Seed in Tests).
     *
     * @param sender    Versand an menschliche Spieler
     * @param scheduler Ausführung zeitversetzter Aufgaben im Server-Thread
     * @param game      Spiel
     */
    public ServerGameLogic(ServerSender sender, ServerScheduler scheduler, Game game) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.game = Objects.requireNonNull(game, "game");
        this.bots = new BotManager(this);
        setState(new LobbyState(this));
    }

    // ------------------------------------------------------------------
    // Einstiegspunkte (vom Netzwerk bzw. Scheduler aufgerufen)
    // ------------------------------------------------------------------

    /**
     * Ein Client hat sich verbunden.
     *
     * @param id Verbindungs-ID
     * @return {@code false}, wenn der Client abgewiesen werden muss (Spiel voll oder läuft bereits)
     */
    public boolean connectionAdded(int id) {
        return state.playerConnected(id);
    }

    /**
     * Ein Client hat die Verbindung getrennt.
     *
     * @param id Verbindungs-ID
     */
    public void connectionRemoved(int id) {
        state.playerDisconnected(id);
    }

    /**
     * Verarbeitet eine Nachricht eines Spielers (Mensch oder Bot).
     *
     * @param msg  Nachricht
     * @param from ID des Absenders
     */
    public void receive(ClientMessage msg, int from) {
        LOGGER.log(Level.DEBUG, "received {0} from {1}", msg, from);
        if (msg instanceof CMAnimationsDone done) {
            acknowledge(done.getSyncId(), from);
            return;
        }
        if (pendingRequest != null && pendingRequest.accepts(msg, from)) {
            PendingRequest<?> request = pendingRequest;
            pendingRequest = null;
            request.handle(msg);
            return;
        }
        if (isBusy()) {
            LOGGER.log(Level.WARNING, "ignoriere {0} von {1}: es läuft noch ein Ablauf", msg, from);
            return;
        }
        msg.accept(state, from);
    }

    // ------------------------------------------------------------------
    // Zustand
    // ------------------------------------------------------------------

    /**
     * @return aktueller Zustand
     */
    public ServerState getState() {
        return state;
    }

    /**
     * Wechselt den Zustand und ruft dessen {@link ServerState#entry()} auf.
     *
     * @param newState neuer Zustand
     */
    public void setState(ServerState newState) {
        LOGGER.log(Level.DEBUG, "Zustandswechsel {0} --> {1}", state, newState);
        state = newState;
        state.entry();
    }

    /**
     * @return Spiel
     */
    public Game getGame() {
        return game;
    }

    /**
     * @return Spieler in Sitzreihenfolge
     */
    public List<Player> getPlayers() {
        return game.getPlayers();
    }

    /**
     * @param id Spieler-ID
     * @return Spieler mit dieser ID
     */
    public Optional<Player> findPlayer(int id) {
        return game.findPlayer(id);
    }

    /**
     * @param name Spielername
     * @return Spieler mit diesem Namen
     */
    public Optional<Player> findPlayer(String name) {
        return game.findPlayer(name);
    }

    /**
     * @return Bot-Verwaltung
     */
    public BotManager getBots() {
        return bots;
    }

    /**
     * @return Scheduler für zeitversetzte Aufgaben
     */
    public ServerScheduler getScheduler() {
        return scheduler;
    }

    /**
     * @return Zug-Informationen (Aktionspunkte, verbrauchte Helden)
     */
    public TurnInfo getTurn() {
        return turn;
    }

    /**
     * @return {@code true}, wenn ein Broadcast oder eine Anfrage aussteht
     */
    public boolean isBusy() {
        return pendingSync != null || pendingRequest != null;
    }

    /**
     * Legt fest, was nach Spielende passieren soll (z. B. Server beenden).
     *
     * @param onGameFinished Aktion
     */
    public void setOnGameFinished(Runnable onGameFinished) {
        this.onGameFinished = Objects.requireNonNull(onGameFinished);
    }

    /**
     * Meldet das Spielende an den Betreiber (z. B. den Server).
     */
    public void notifyGameFinished() {
        onGameFinished.run();
    }

    // ------------------------------------------------------------------
    // Spieler & Lobby
    // ------------------------------------------------------------------

    /**
     * Fügt einen menschlichen Spieler hinzu. Der erste Mensch wird Spielleiter.
     *
     * @param id Verbindungs-ID
     * @return neuer Spieler
     */
    public Player addHuman(int id) {
        Player player = new Player(id, uniqueName("Spieler " + (getPlayers().size() + 1)), false);
        game.addPlayer(player);
        if (hostId == Integer.MIN_VALUE) hostId = id;
        LOGGER.log(Level.INFO, "Spieler hinzugefügt: {0}", player);
        return player;
    }

    /**
     * Fügt einen Bot hinzu.
     *
     * @param level Spielstärke
     * @return neuer Bot
     */
    public Player addBot(BotLevel level) {
        Player bot = new Player(nextBotId--, uniqueName(bots.nextName()), true);
        game.addPlayer(bot);
        bots.register(bot, level);
        LOGGER.log(Level.INFO, "Bot hinzugefügt: {0} ({1})", bot, level);
        return bot;
    }

    /**
     * Entfernt einen Spieler aus der Lobby. Verlässt der Spielleiter die Lobby, übernimmt
     * der nächste menschliche Spieler.
     *
     * @param player Spieler
     */
    public void removePlayer(Player player) {
        game.removePlayer(player);
        bots.unregister(player);
        LOGGER.log(Level.INFO, "Spieler entfernt: {0}", player);
        if (player.getId() == hostId) {
            // Die Spielleitung geht an den nächsten Menschen über.
            hostId = getPlayers().stream().filter(p -> !p.isBot()).mapToInt(Player::getId).findFirst()
                                 .orElse(Integer.MIN_VALUE);
        }
    }

    /**
     * @param id Spieler-ID
     * @return {@code true}, wenn der Spieler die Partie eröffnet hat
     */
    public boolean isHost(int id) {
        return id == hostId;
    }

    /**
     * Macht aus einem Namen einen eindeutigen Namen, indem bei Bedarf eine
     * Nummer angehängt wird.
     *
     * @param wanted gewünschter Name
     * @return eindeutiger Name
     */
    public String uniqueName(String wanted) {
        String base = wanted;
        String candidate = base;
        int counter = 2;
        while (findPlayer(candidate).isPresent()) {
            candidate = base + " " + counter++;
        }
        return candidate;
    }

    /**
     * Sendet die aktuelle Spielerliste an alle menschlichen Spieler.
     */
    public void broadcastLobby() {
        List<LobbyEntry> entries = new ArrayList<>();
        for (Player p : getPlayers()) {
            entries.add(new LobbyEntry(p.getName(), p.isBot(), isHost(p.getId()), bots.levelOf(p)));
        }
        for (Player p : getPlayers()) {
            send(p, new SMLobbyUpdate(entries));
        }
    }

    /**
     * Übergibt einen Spieler, der die Verbindung verloren hat, an einen Bot.
     * Offene Bestätigungen und Anfragen werden an den Bot weitergereicht.
     *
     * @param player Spieler
     */
    public void replaceByBot(Player player) {
        player.replaceByBot();
        bots.register(player, BotLevel.NORMAL);
        sendToAll(new SMNotice(player.getName() + " hat das Spiel verlassen – ein Bot übernimmt."));
        if (pendingSync != null && pendingSync.waitingFor.contains(player.getId()))
            acknowledge(pendingSync.syncId, player.getId());
        if (pendingRequest != null && pendingRequest.playerId == player.getId())
            send(player, pendingRequest.request);
    }

    /**
     * @return {@code true}, wenn noch mindestens ein Mensch mitspielt
     */
    public boolean hasHumans() {
        return getPlayers().stream().anyMatch(p -> !p.isBot());
    }

    // ------------------------------------------------------------------
    // Zugreihenfolge
    // ------------------------------------------------------------------

    /**
     * @return Spieler, der am Zug ist
     */
    public Player getActivePlayer() {
        return getPlayers().get(activeIndex);
    }

    /**
     * @return Index des Spielers, der am Zug ist
     */
    public int getActiveIndex() {
        return activeIndex;
    }

    /**
     * Setzt den aktiven Spieler und füllt dessen Aktionspunkte auf.
     *
     * @param index Index in der Sitzreihenfolge
     */
    public void beginTurn(int index) {
        activeIndex = index;
        turn.reset();
    }

    /**
     * @return Index des Spielers, der nach dem aktiven Spieler an der Reihe ist
     */
    public int nextIndex() {
        return (activeIndex + 1) % getPlayers().size();
    }

    // ------------------------------------------------------------------
    // Kommunikation
    // ------------------------------------------------------------------

    /**
     * Sendet eine Nachricht an einen Spieler (an Bots direkt, an Menschen übers Netz).
     *
     * @param player Empfänger
     * @param msg    Nachricht
     */
    public void send(Player player, ServerMessage msg) {
        LOGGER.log(Level.DEBUG, "sende an {0}: {1}", player.getName(), msg);
        if (player.isBot())
            bots.deliver(player, msg);
        else
            sender.send(player.getId(), msg);
    }

    /**
     * Sendet dieselbe Nachricht an alle Spieler.
     *
     * @param msg Nachricht (darf keine {@link BroadcastMessage} sein)
     */
    public void sendToAll(ServerMessage msg) {
        if (msg instanceof BroadcastMessage)
            throw new IllegalArgumentException("Broadcasts bitte mit broadcast(...) senden");
        getPlayers().forEach(p -> send(p, msg));
    }

    /**
     * Sendet ein Spielereignis an alle Spieler. {@code afterAll} wird ausgeführt,
     * sobald alle Spieler die Animationen bestätigt haben.
     *
     * @param msg      Ereignis
     * @param afterAll Fortsetzung des Spielablaufs
     */
    public void broadcast(BroadcastMessage msg, Runnable afterAll) {
        if (pendingSync != null)
            throw new IllegalStateException("Es läuft bereits ein Broadcast (" + pendingSync.syncId + ")");
        int syncId = nextSyncId++;
        Set<Integer> waitingFor = new HashSet<>();
        getPlayers().forEach(p -> waitingFor.add(p.getId()));
        pendingSync = new PendingSync(syncId, waitingFor, afterAll);
        for (Player p : List.copyOf(getPlayers())) {
            send(p, msg.personalizedFor(syncId, p.getName(), createSync(p)));
        }
        scheduler.schedule(() -> syncTimeout(syncId), SYNC_TIMEOUT_MILLIS);
    }

    /**
     * Stellt einem Spieler eine Frage und wartet auf seine Antwort.
     *
     * @param player       gefragter Spieler
     * @param request      Anfrage
     * @param responseType erwarteter Antworttyp
     * @param onResponse   Verarbeitung der Antwort
     * @param <T>          Antworttyp
     */
    public <T extends ClientMessage> void request(Player player, ServerMessage request, Class<T> responseType,
                                                  Consumer<T> onResponse) {
        if (pendingRequest != null)
            throw new IllegalStateException("Es läuft bereits eine Anfrage an " + pendingRequest.playerId);
        pendingRequest = new PendingRequest<>(player.getId(), request, responseType, onResponse);
        send(player, request);
    }

    private void acknowledge(int syncId, int from) {
        if (pendingSync == null || pendingSync.syncId != syncId) {
            LOGGER.log(Level.DEBUG, "veraltete Bestätigung {0} von {1}", syncId, from);
            return;
        }
        pendingSync.waitingFor.remove(from);
        if (pendingSync.waitingFor.isEmpty()) {
            Runnable continuation = pendingSync.continuation;
            pendingSync = null;
            continuation.run();
        }
    }

    private void syncTimeout(int syncId) {
        if (pendingSync != null && pendingSync.syncId == syncId) {
            LOGGER.log(Level.WARNING, "Zeitüberschreitung beim Broadcast {0}; fehlende Bestätigungen von {1}",
                       syncId, pendingSync.waitingFor);
            for (Integer id : List.copyOf(pendingSync.waitingFor)) {
                acknowledge(syncId, id);
            }
        }
    }

    /**
     * Erzeugt den Spielzustand aus Sicht eines Spielers.
     *
     * @param viewer Spieler
     * @return persönliche Sicht
     */
    public ClientSync createSync(Player viewer) {
        List<PlayerSnapshot> snapshots = new ArrayList<>();
        for (Player p : getPlayers()) {
            snapshots.add(new PlayerSnapshot(p.getName(), p.isBot(), p.getHand().size(),
                                             p.getHover().getCard().orElse(null),
                                             p.getGroup().getSlots(), p.getMonsters().getSlots(),
                                             p.getLeader(), p.getAttackBonus(), p.getChallengeBonus(),
                                             p.getHeroBonus()));
        }
        List<Card> discard = game.getDiscardPile();
        String active = state != null && state.isPlaying() ? getActivePlayer().getName() : null;
        return new ClientSync(viewer.getName(), snapshots, viewer.getHand().getCards(), active,
                              discard.isEmpty() ? null : discard.getLast(), discard.size(),
                              game.getSupportDeck().size(), game.getMonsterDeck().size(),
                              game.getOpenMonsters().getSlots());
    }

    /**
     * Offener Broadcast, der auf Bestätigungen wartet.
     */
    private static final class PendingSync {
        private final int syncId;
        private final Set<Integer> waitingFor;
        private final Runnable continuation;

        private PendingSync(int syncId, Set<Integer> waitingFor, Runnable continuation) {
            this.syncId = syncId;
            this.waitingFor = waitingFor;
            this.continuation = continuation;
        }
    }

    /**
     * Offene Anfrage an einen Spieler.
     *
     * @param <T> erwarteter Antworttyp
     */
    private static final class PendingRequest<T extends ClientMessage> {
        private final int playerId;
        private final ServerMessage request;
        private final Class<T> responseType;
        private final Consumer<T> handler;

        private PendingRequest(int playerId, ServerMessage request, Class<T> responseType, Consumer<T> handler) {
            this.playerId = playerId;
            this.request = request;
            this.responseType = responseType;
            this.handler = handler;
        }

        boolean accepts(ClientMessage msg, int from) {
            return from == playerId && responseType.isInstance(msg);
        }

        void handle(ClientMessage msg) {
            handler.accept(responseType.cast(msg));
        }
    }
}
