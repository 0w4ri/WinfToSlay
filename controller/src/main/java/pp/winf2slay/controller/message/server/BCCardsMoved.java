package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.controller.message.CardPosition;
import pp.winf2slay.controller.message.MoveCause;
import pp.winf2slay.model.card.Card;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mehrere Karten mehrerer Spieler wurden gleichzeitig bewegt
 * (z. B. Bombe, Helikopter, „Jeder wirft ab“).
 *
 * <p>Broadcast-Nachricht: Jeder Client bestätigt sie nach den Animationen.</p>
 */
@Serializable
public class BCCardsMoved extends BroadcastMessage {

    private List<String> playerNames;
    private List<List<Card>> cards;
    private CardPosition from;
    private CardPosition to;
    private MoveCause cause;

    private BCCardsMoved() {
        // für @Serializable
    }

    /**
     * @param playerNames betroffene Spieler
     * @param cards Karten je Spieler (gleiche Reihenfolge wie die Namen)
     * @param from Herkunft
     * @param to Ziel
     * @param cause Grund der Bewegung
     */
    public BCCardsMoved(List<String> playerNames, List<List<Card>> cards, CardPosition from, CardPosition to, MoveCause cause) {
        this.playerNames = new ArrayList<>(playerNames);
        this.cards = new ArrayList<>(cards);
        this.from = from;
        this.to = to;
        this.cause = cause;
    }

    /**
     * @return betroffene Spieler
     */
    public List<String> getPlayerNames() {
        return playerNames;
    }

    /**
     * @return Karten je Spieler (gleiche Reihenfolge wie die Namen)
     */
    public List<List<Card>> getCards() {
        return cards;
    }

    /**
     * @return Herkunft
     */
    public CardPosition getFrom() {
        return from;
    }

    /**
     * @return Ziel
     */
    public CardPosition getTo() {
        return to;
    }

    /**
     * @return Grund der Bewegung
     */
    public MoveCause getCause() {
        return cause;
    }

    /**
     * Erzeugt die Nachricht aus einer Zuordnung Spieler → Karten.
     *
     * @param cardsByPlayer bewegte Karten je Spielername
     * @param from          Herkunft
     * @param to            Ziel
     * @param cause         Grund der Bewegung
     * @return Nachricht
     */
    public static BCCardsMoved of(Map<String, ? extends List<? extends Card>> cardsByPlayer, CardPosition from, CardPosition to, MoveCause cause) {
        List<String> names = new ArrayList<>();
        List<List<Card>> cards = new ArrayList<>();
        cardsByPlayer.forEach((name, list) -> {
            names.add(name);
            cards.add(new ArrayList<>(list));
        });
        return new BCCardsMoved(names, cards, from, to, cause);
    }

    /**
     * @return bewegte Karten je Spielername (in Sitzreihenfolge)
     */
    public Map<String, List<Card>> getCardsByPlayer() {
        Map<String, List<Card>> map = new LinkedHashMap<>();
        for (int i = 0; i < playerNames.size(); i++) {
            map.put(playerNames.get(i), cards.get(i));
        }
        return map;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "BCCardsMoved[" + "playerNames=" + playerNames + ", " + "cards=" + cards + ", " + "from=" + from + ", " + "to=" + to + ", " + "cause=" + cause + "]";
    }
}
