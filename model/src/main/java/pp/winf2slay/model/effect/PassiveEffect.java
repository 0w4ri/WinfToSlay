package pp.winf2slay.model.effect;

import pp.winf2slay.model.Player;

/**
 * Dauerhafter Würfelbonus, den ein Spieler durch seinen Anführer oder ein
 * besiegtes Monster erhält.
 */
public sealed interface PassiveEffect extends Effect permits AttackBonus, ChallengeBonus, HeroBonus {

    /**
     * Wendet den Bonus dauerhaft auf einen Spieler an.
     *
     * @param player begünstigter Spieler
     */
    void applyTo(Player player);

    /**
     * @return Höhe des Bonus
     */
    int getBonus();
}
