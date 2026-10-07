package pp.winf2slay.model.effect;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.Player;

/**
 * Dauerhafter Bonus auf alle Würfe bei Herausforderungen.
 */
@Serializable
public final class ChallengeBonus implements PassiveEffect {

    private int bonus;

    private ChallengeBonus() {
        // für @Serializable
    }

    /**
     * @param bonus Höhe des Bonus (nicht negativ)
     */
    public ChallengeBonus(int bonus) {
        this.bonus = bonus;
    }

    @Override
    public void applyTo(Player player) {
        player.addChallengeBonus(bonus);
    }

    @Override
    public int getBonus() {
        return bonus;
    }

    @Override
    public String describe() {
        return "+" + bonus + " auf alle Würfelergebnisse bei Herausforderungen.";
    }
}
