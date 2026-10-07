package pp.winf2slay.model.effect;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Hero;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * „Zerstöre alle ausgespielten Helden.“ – Alle Helden aller Spieler (auch die
 * eigenen) landen auf dem Ablagestapel.
 */
@Serializable
public final class DestroyAllHeroes implements Effect {

    /**
     * Erzeugt den Effekt.
     */
    public DestroyAllHeroes() {
        // keine Parameter
    }

    /**
     * Zerstört alle Helden.
     *
     * @param game Spiel
     * @return zerstörte Helden je Spieler (nur Spieler mit mindestens einem Helden)
     */
    public Map<Player, List<Hero>> destroyAll(Game game) {
        Map<Player, List<Hero>> destroyed = new LinkedHashMap<>();
        for (Player player : game.getPlayers()) {
            List<Hero> heroes = player.getGroup().getHeroes();
            if (heroes.isEmpty()) continue;
            heroes.forEach(h -> game.moveGroupToDiscard(player, h));
            destroyed.put(player, heroes);
        }
        return destroyed;
    }

    @Override
    public String describe() {
        return "Zerstöre alle ausgespielten Helden.";
    }
}
