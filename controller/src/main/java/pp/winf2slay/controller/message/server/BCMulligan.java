package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * Ein Spieler hat einen Mulligan durchgeführt.
 *
 * <p>Broadcast-Nachricht: Jeder Client bestätigt sie nach den Animationen.</p>
 */
@Serializable
public class BCMulligan extends BroadcastMessage {

    private String playerName;
    private List<Card> discarded;
    private List<Card> drawn;
    private int drawnCount;

    private BCMulligan() {
        // für @Serializable
    }

    /**
     * @param playerName Spieler
     * @param discarded abgelegte Karten
     * @param drawn neu gezogene Karten
     */
    public BCMulligan(String playerName, List<Card> discarded, List<Card> drawn) {
        this.playerName = playerName;
        this.discarded = new ArrayList<>(discarded);
        this.drawn = new ArrayList<>(drawn);
    }

    /**
     * @return Spieler
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * @return abgelegte Karten
     */
    public List<Card> getDiscarded() {
        return discarded;
    }

    /**
     * @return neu gezogene Karten
     */
    public List<Card> getDrawn() {
        return drawn;
    }

    /**
     * @return Anzahl der neu gezogenen Karten (auch für Gegner sichtbar)
     */
    public int getDrawnCount() {
        return drawn.size() > 0 ? drawn.size() : drawnCount;
    }

    /**
     * Die neu gezogenen Karten sind nur für den Spieler selbst sichtbar.
     *
     * @param viewer Name des Empfängers
     */
    @Override
    protected void hideFrom(String viewer) {
        if (!viewer.equals(playerName)) {
            drawnCount = drawn.size();
            drawn = new ArrayList<>();
        }
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "BCMulligan[" + "playerName=" + playerName + ", " + "discarded=" + discarded + ", " + "drawn=" + drawn + "]";
    }
}
