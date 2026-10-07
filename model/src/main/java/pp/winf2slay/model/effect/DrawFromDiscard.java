package pp.winf2slay.model.effect;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Card;

import java.util.Optional;

/**
 * „Nimm n Karten vom Ablagestapel.“ – Die jeweils oberste Karte des
 * Ablagestapels wandert auf die Hand des ausführenden Spielers.
 */
@Serializable
public final class DrawFromDiscard implements Effect {

    private int count;

    private DrawFromDiscard() {
        // für @Serializable
    }

    /**
     * @param count Anzahl der aufzunehmenden Karten
     */
    public DrawFromDiscard(int count) {
        this.count = count;
    }

    /**
     * Nimmt die oberste Karte des Ablagestapels auf.
     *
     * @param game   Spiel
     * @param player aufnehmender Spieler
     * @return aufgenommene Karte; leer, wenn der Ablagestapel leer ist
     */
    public Optional<Card> takeOne(Game game, Player player) {
        return game.takeFromDiscard(player);
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public String describe() {
        return count == 1
               ? "Nimm die oberste Karte des Ablagestapels."
               : "Nimm die obersten " + count + " Karten des Ablagestapels.";
    }
}
