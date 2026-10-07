package pp.winf2slay.model.effect;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.Player;

/**
 * Dauerhafter Bonus auf alle Würfe, mit denen Heldeneffekte ausgelöst werden.
 */
@Serializable
public final class HeroBonus implements PassiveEffect {

    private int bonus;

    private HeroBonus() {
        // für @Serializable
    }

    /**
     * @param bonus Höhe des Bonus (nicht negativ)
     */
    public HeroBonus(int bonus) {
        this.bonus = bonus;
    }

    @Override
    public void applyTo(Player player) {
        player.addHeroBonus(bonus);
    }

    @Override
    public int getBonus() {
        return bonus;
    }

    @Override
    public String describe() {
        return "+" + bonus + " auf alle Heldeneffekte.";
    }
}
