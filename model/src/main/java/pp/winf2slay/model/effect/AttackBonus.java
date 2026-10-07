package pp.winf2slay.model.effect;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.Player;

/**
 * Dauerhafter Bonus auf alle Angriffswürfe gegen Monster.
 */
@Serializable
public final class AttackBonus implements PassiveEffect {

    private int bonus;

    private AttackBonus() {
        // für @Serializable
    }

    /**
     * @param bonus Höhe des Bonus (nicht negativ)
     */
    public AttackBonus(int bonus) {
        this.bonus = bonus;
    }

    @Override
    public void applyTo(Player player) {
        player.addAttackBonus(bonus);
    }

    @Override
    public int getBonus() {
        return bonus;
    }

    @Override
    public String describe() {
        return "+" + bonus + " auf alle Würfelergebnisse bei Monsterangriffen.";
    }
}
