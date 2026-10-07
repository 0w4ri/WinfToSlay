package pp.winf2slay.model.effect;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Hero;

import java.util.List;

/**
 * „Zerstöre alle Helden eines ausgewählten Spielers.“
 */
@Serializable
public final class DestroyParty implements Effect {

    /**
     * Erzeugt den Effekt.
     */
    public DestroyParty() {
        // keine Parameter
    }

    /**
     * Liefert alle Spieler, deren Gruppe mindestens einen Helden enthält.
     *
     * @param game Spiel
     * @return mögliche Ziele (kann auch den ausführenden Spieler enthalten)
     */
    public List<Player> targets(Game game) {
        return game.getPlayers().stream().filter(p -> !p.getGroup().isEmpty()).toList();
    }

    /**
     * Zerstört alle Helden des Zielspielers.
     *
     * @param game   Spiel
     * @param target Zielspieler
     * @return zerstörte Helden
     */
    public List<Hero> destroyParty(Game game, Player target) {
        List<Hero> heroes = target.getGroup().getHeroes();
        heroes.forEach(h -> game.moveGroupToDiscard(target, h));
        return heroes;
    }

    @Override
    public String describe() {
        return "Zerstöre alle Helden eines ausgewählten Spielers.";
    }
}
