package pp.winf2slay.controller.client;

import pp.winf2slay.controller.message.LobbyEntry;
import pp.winf2slay.controller.message.server.ClientSync;
import pp.winf2slay.controller.message.server.PlayerSnapshot;
import pp.winf2slay.model.Action;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.field.MonsterField;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Lokaler Spielzustand eines Clients.
 *
 * <p>Der Zustand wird nach jedem Broadcast vollständig aus der
 * {@link ClientSync} des Servers übernommen; der Client rechnet nichts
 * selbst nach. Nur Aktionspunkte und bereits genutzte Helden werden lokal
 * mitgeführt, damit die Oberfläche sofort reagieren kann.</p>
 */
public class ClientGameState {

    private String myName = "";
    private List<LobbyEntry> lobby = new ArrayList<>();
    private boolean host;

    private List<PlayerSnapshot> players = new ArrayList<>();
    private List<Card> hand = new ArrayList<>();
    private String activePlayer;
    private Card discardTop;
    private int discardCount;
    private int supportCount;
    private int monsterDeckCount;
    private Monster[] openMonsters = new Monster[MonsterField.SIZE];
    private int actionPoints;
    private final Set<String> usedHeroes = new HashSet<>();

    /**
     * Übernimmt den Spielzustand des Servers.
     *
     * @param sync Spielzustand aus Sicht dieses Clients
     */
    public void apply(ClientSync sync) {
        if (sync == null) return;
        players = new ArrayList<>(sync.getPlayers());
        hand = new ArrayList<>(sync.getHand());
        if (sync.getActivePlayer() != null) activePlayer = sync.getActivePlayer();
        discardTop = sync.getDiscardTop();
        discardCount = sync.getDiscardCount();
        supportCount = sync.getSupportCount();
        monsterDeckCount = sync.getMonsterDeckCount();
        openMonsters = sync.getOpenMonsters();
    }

    // ------------------------------------------------------------------
    // Lobby
    // ------------------------------------------------------------------

    /**
     * @return eigener Spielername
     */
    public String getMyName() {
        return myName;
    }

    /**
     * @param myName eigener Spielername
     */
    public void setMyName(String myName) {
        this.myName = myName;
    }

    /**
     * @return Spielerliste der Lobby
     */
    public List<LobbyEntry> getLobby() {
        return lobby;
    }

    /**
     * @param lobby neue Spielerliste
     */
    public void setLobby(List<LobbyEntry> lobby) {
        this.lobby = new ArrayList<>(lobby);
        this.host = lobby.stream().anyMatch(e -> e.isHost() && e.getName().equals(myName));
    }

    /**
     * @return {@code true}, wenn dieser Client die Partie leitet
     */
    public boolean isHost() {
        return host;
    }

    // ------------------------------------------------------------------
    // Spiel
    // ------------------------------------------------------------------

    /**
     * @return alle Spieler in Sitzreihenfolge
     */
    public List<PlayerSnapshot> getPlayers() {
        return players;
    }

    /**
     * @param name Spielername
     * @return Zustand des Spielers
     */
    public Optional<PlayerSnapshot> getPlayer(String name) {
        return players.stream().filter(p -> p.getName().equals(name)).findFirst();
    }

    /**
     * @return eigener Zustand (leer vor Spielbeginn)
     */
    public Optional<PlayerSnapshot> getMe() {
        return getPlayer(myName);
    }

    /**
     * Liefert die Gegner in Sitzreihenfolge, beginnend mit dem linken Nachbarn.
     *
     * @return Gegner
     */
    public List<PlayerSnapshot> getOpponents() {
        List<PlayerSnapshot> result = new ArrayList<>();
        int me = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equals(myName)) me = i;
        }
        for (int i = 1; i < players.size(); i++) {
            result.add(players.get((Math.max(me, 0) + i) % players.size()));
        }
        if (me < 0 && !players.isEmpty()) result.addFirst(players.getFirst());
        return result;
    }

    /**
     * @return eigene Handkarten
     */
    public List<Card> getHand() {
        return hand;
    }

    /**
     * @return Spieler am Zug
     */
    public String getActivePlayer() {
        return activePlayer;
    }

    /**
     * @param activePlayer Spieler am Zug
     */
    public void setActivePlayer(String activePlayer) {
        this.activePlayer = activePlayer;
    }

    /**
     * @return {@code true}, wenn dieser Client am Zug ist
     */
    public boolean isMyTurn() {
        return myName.equals(activePlayer);
    }

    /**
     * @return oberste Karte des Ablagestapels oder {@code null}
     */
    public Card getDiscardTop() {
        return discardTop;
    }

    /**
     * @return Größe des Ablagestapels
     */
    public int getDiscardCount() {
        return discardCount;
    }

    /**
     * @return Größe des Nachziehstapels
     */
    public int getSupportCount() {
        return supportCount;
    }

    /**
     * @return Größe des Monsterstapels
     */
    public int getMonsterDeckCount() {
        return monsterDeckCount;
    }

    /**
     * @return offen ausliegende Monster ({@code null} = frei)
     */
    public Monster[] getOpenMonsters() {
        return openMonsters;
    }

    // ------------------------------------------------------------------
    // Zug
    // ------------------------------------------------------------------

    /**
     * @return verbleibende Aktionspunkte im eigenen Zug
     */
    public int getActionPoints() {
        return actionPoints;
    }

    /**
     * @param actionPoints verbleibende Aktionspunkte
     */
    public void setActionPoints(int actionPoints) {
        this.actionPoints = actionPoints;
    }

    /**
     * Beginnt einen eigenen Zug.
     */
    public void startMyTurn() {
        actionPoints = Action.POINTS_PER_TURN;
        usedHeroes.clear();
    }

    /**
     * @param hero Held
     * @return {@code true}, wenn sein Effekt in diesem Zug schon genutzt wurde
     */
    public boolean isHeroUsed(Hero hero) {
        return usedHeroes.contains(hero.getName());
    }

    /**
     * @param hero Held, dessen Effekt genutzt wurde
     */
    public void markHeroUsed(Hero hero) {
        usedHeroes.add(hero.getName());
    }
}
