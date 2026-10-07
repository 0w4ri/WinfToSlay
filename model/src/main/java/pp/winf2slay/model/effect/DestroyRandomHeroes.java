package pp.winf2slay.model.effect;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Hero;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * „Zerstöre n zufällige, ausgespielte Helden.“ – Das Ziel wird zufällig unter
 * allen Helden aller Spieler bestimmt (auch den eigenen).
 */
@Serializable
public final class DestroyRandomHeroes implements Effect {

    private int count;

    private DestroyRandomHeroes() {
        // für @Serializable
    }

    /**
     * @param count Anzahl der zu zerstörenden Helden
     */
    public DestroyRandomHeroes(int count) {
        this.count = count;
    }

    /**
     * Ergebnis einer zufälligen Zerstörung.
     *
     * @param hero  zerstörter Held
     * @param owner bisheriger Besitzer
     */
    public record Destroyed(Hero hero, Player owner) {}

    /**
     * Zerstört genau einen zufälligen Helden.
     *
     * @param game Spiel (liefert auch den Zufallsgenerator)
     * @return zerstörter Held mit Besitzer; leer, wenn niemand Helden besitzt
     */
    public Optional<Destroyed> destroyOne(Game game) {
        List<Destroyed> candidates = new ArrayList<>();
        for (Player player : game.getPlayers()) {
            for (Hero hero : player.getGroup().getHeroes()) {
                candidates.add(new Destroyed(hero, player));
            }
        }
        if (candidates.isEmpty()) return Optional.empty();
        Destroyed pick = candidates.get(game.getRandom().nextInt(candidates.size()));
        game.moveGroupToDiscard(pick.owner(), pick.hero());
        return Optional.of(pick);
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public String describe() {
        return count == 1
               ? "Zerstöre einen zufälligen, ausgespielten Helden."
               : "Zerstöre " + count + " zufällige, ausgespielte Helden.";
    }
}
