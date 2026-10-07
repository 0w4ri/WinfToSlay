package pp.winf2slay.model.effect;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Card;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * „Jeder Spieler wirft n zufällige Handkarten ab.“
 */
@Serializable
public final class EveryoneDiscards implements Effect {

    private int count;

    private EveryoneDiscards() {
        // für @Serializable
    }

    /**
     * @param count Anzahl der Karten, die jeder Spieler abwirft
     */
    public EveryoneDiscards(int count) {
        this.count = count;
    }

    /**
     * Jeder Spieler mit Handkarten wirft genau eine zufällige Karte ab.
     *
     * @param game Spiel (liefert den Zufallsgenerator)
     * @return abgeworfene Karte je Spieler (nur Spieler mit Handkarten)
     */
    public Map<Player, Card> discardOneEach(Game game) {
        Map<Player, Card> discarded = new LinkedHashMap<>();
        for (Player player : game.getPlayers()) {
            List<Card> hand = player.getHand().getCards();
            if (hand.isEmpty()) continue;
            Card card = hand.get(game.getRandom().nextInt(hand.size()));
            game.moveHandToDiscard(player, card);
            discarded.put(player, card);
        }
        return discarded;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public String describe() {
        return count == 1
               ? "Jeder Spieler wirft eine zufällige Karte ab."
               : "Jeder Spieler wirft " + count + " zufällige Karten ab.";
    }
}
