package pp.winf2slay.model.die;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiceTest {

    @RepeatedTest(20)
    void dieStaysInRange() {
        Die die = new Die(new Random());
        int value = die.roll();
        assertTrue(value >= 1 && value <= 6, "Wurf außerhalb 1..6: " + value);
    }

    @Test
    void totalCountsBonusAndModificationExactlyOnce() {
        // Früher wurde der Bonus doppelt gezählt (einmal im ersten Würfel, einmal im Total).
        DiceResult result = new DiceResult("A", 3, 4, 1, 0);
        assertEquals(8, result.getTotal());
        DiceResult modified = result.modifiedBy(2).modifiedBy(-4);
        assertEquals(6, modified.getTotal());
        assertEquals(-2, modified.getModification());
        assertEquals(-1, modified.getAdjustment());
        // Das Original bleibt unverändert
        assertEquals(8, result.getTotal());
        assertEquals(3, modified.getDie1());
        assertEquals(4, modified.getDie2());
    }

    @Test
    void pairOfDiceIsReproducible() {
        DiceResult a = new PairOfDice(new Random(7)).roll("A", 0);
        DiceResult b = new PairOfDice(new Random(7)).roll("A", 0);
        assertEquals(a.getDie1(), b.getDie1());
        assertEquals(a.getDie2(), b.getDie2());
    }
}
