package pp.winf2slay.model.effect;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Hero;

import java.util.List;
import java.util.Optional;

/**
 * „Opfere einen Helden.“ – Strafe für einen verlorenen Monsterkampf: Ein
 * zufälliger Held aus der Gruppe des Angreifers wird zerstört.
 */
@Serializable
public final class SacrificeHero implements Effect {

    /**
     * Erzeugt den Effekt.
     */
    public SacrificeHero() {
        // keine Parameter
    }

    /**
     * Opfert einen zufälligen Helden des Spielers.
     *
     * @param game   Spiel (liefert den Zufallsgenerator)
     * @param player betroffener Spieler
     * @return geopferter Held; leer, wenn der Spieler keine Helden hat
     */
    public Optional<Hero> sacrifice(Game game, Player player) {
        List<Hero> heroes = player.getGroup().getHeroes();
        if (heroes.isEmpty()) return Optional.empty();
        Hero victim = heroes.get(game.getRandom().nextInt(heroes.size()));
        game.moveGroupToDiscard(player, victim);
        return Optional.of(victim);
    }

    @Override
    public String describe() {
        return "Opfere einen Helden.";
    }
}
