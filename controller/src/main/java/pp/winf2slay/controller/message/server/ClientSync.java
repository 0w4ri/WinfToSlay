package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Monster;

import java.util.ArrayList;
import java.util.List;

/**
 * Spielzustand aus Sicht eines bestimmten Spielers.
 *
 * <p>Jeder Broadcast enthält eine solche Momentaufnahme, damit die Clients nach
 * jedem Ereignis exakt den Serverzustand übernehmen können. Die Handkarten
 * der Gegner sind nur als Anzahl enthalten.</p>
 */
@Serializable
public class ClientSync {

    private String viewer;
    private List<PlayerSnapshot> players;
    private List<Card> hand;
    private String activePlayer;
    private Card discardTop;
    private int discardCount;
    private int supportCount;
    private int monsterDeckCount;
    private Monster[] openMonsters;

    private ClientSync() {
        // für @Serializable
    }

    /**
     * @param viewer           Spieler, für den diese Sicht erzeugt wurde
     * @param players          alle Spieler in Sitzreihenfolge
     * @param hand             Handkarten des Empfängers
     * @param activePlayer     Spieler am Zug (oder {@code null} vor Spielbeginn)
     * @param discardTop       oberste Karte des Ablagestapels oder {@code null}
     * @param discardCount     Größe des Ablagestapels
     * @param supportCount     Größe des Unterstützungsstapels
     * @param monsterDeckCount Größe des Monsterstapels
     * @param openMonsters     offen ausliegende Monster ({@code null} = frei)
     */
    public ClientSync(String viewer, List<PlayerSnapshot> players, List<Card> hand, String activePlayer,
                      Card discardTop, int discardCount, int supportCount, int monsterDeckCount,
                      Monster[] openMonsters) {
        this.viewer = viewer;
        this.players = new ArrayList<>(players);
        this.hand = new ArrayList<>(hand);
        this.activePlayer = activePlayer;
        this.discardTop = discardTop;
        this.discardCount = discardCount;
        this.supportCount = supportCount;
        this.monsterDeckCount = monsterDeckCount;
        this.openMonsters = openMonsters;
    }

    /**
     * @return Spieler, für den diese Sicht erzeugt wurde
     */
    public String getViewer() {
        return viewer;
    }

    /**
     * @return alle Spieler in Sitzreihenfolge
     */
    public List<PlayerSnapshot> getPlayers() {
        return players;
    }

    /**
     * @return Handkarten des Empfängers
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
     * @return Größe des Unterstützungsstapels
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
}
