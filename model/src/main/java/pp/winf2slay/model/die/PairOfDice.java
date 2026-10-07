package pp.winf2slay.model.die;

import java.util.Random;

/**
 * Zwei sechsseitige Würfel, wie sie für alle Würfe in WinfToSlay verwendet werden.
 */
public class PairOfDice {

    private final Die die;

    /**
     * @param rng Zufallsgenerator für beide Würfel
     */
    public PairOfDice(Random rng) {
        this.die = new Die(rng);
    }

    /**
     * Würfelt für einen Spieler.
     *
     * @param playerName Name des würfelnden Spielers
     * @param bonus      passiver Bonus des Spielers für diese Art von Wurf
     * @return Wurfergebnis
     */
    public DiceResult roll(String playerName, int bonus) {
        return new DiceResult(playerName, die.roll(), die.roll(), bonus, 0);
    }
}
