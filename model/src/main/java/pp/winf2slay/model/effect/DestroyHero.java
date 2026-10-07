package pp.winf2slay.model.effect;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Hero;

import java.util.List;
import java.util.Optional;

/**
 * „Zerstöre n ausgewählte Helden.“ – Der ausführende Spieler wählt vor jeder
 * Zerstörung einen Helden eines Gegners aus.
 */
@Serializable
public final class DestroyHero implements Effect {

    private int count;

    private DestroyHero() {
        // für @Serializable
    }

    /**
     * @param count Anzahl der zu zerstörenden Helden
     */
    public DestroyHero(int count) {
        this.count = count;
    }

    /**
     * Liefert alle Helden, die der ausführende Spieler als Ziel wählen darf
     * (alle Helden seiner Gegner).
     *
     * @param game   Spiel
     * @param caster ausführender Spieler
     * @return mögliche Ziele, ggf. leer
     */
    public List<Hero> targetsFor(Game game, Player caster) {
        return game.getHeroesOfOpponents(caster);
    }

    /**
     * Zerstört den gewählten Helden.
     *
     * @param game   Spiel
     * @param target gewählter Held
     * @return bisheriger Besitzer des Helden; leer, wenn der Held nicht mehr im Spiel ist
     */
    public Optional<Player> destroy(Game game, Hero target) {
        Optional<Player> owner = game.findOwner(target);
        owner.ifPresent(p -> game.moveGroupToDiscard(p, target));
        return owner;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public String describe() {
        return count == 1
               ? "Zerstöre einen ausgewählten Helden."
               : "Zerstöre " + count + " ausgewählte Helden.";
    }
}
