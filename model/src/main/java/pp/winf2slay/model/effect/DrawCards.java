package pp.winf2slay.model.effect;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Card;

import java.util.Optional;

/**
 * „Ziehe n Karten.“ – Der ausführende Spieler zieht Karten vom Unterstützungsstapel.
 */
@Serializable
public final class DrawCards implements Effect {

    private int count;

    private DrawCards() {
        // für @Serializable
    }

    /**
     * @param count Anzahl der zu ziehenden Karten
     */
    public DrawCards(int count) {
        this.count = count;
    }

    /**
     * Zieht genau eine Karte.
     *
     * @param game   Spiel
     * @param player ziehender Spieler
     * @return gezogene Karte; leer, wenn kein Stapel mehr Karten hat
     */
    public Optional<Card> drawOne(Game game, Player player) {
        return game.drawSupportCard(player);
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public String describe() {
        return count == 1 ? "Ziehe eine Karte." : "Ziehe " + count + " Karten.";
    }
}
