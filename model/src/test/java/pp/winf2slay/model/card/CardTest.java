package pp.winf2slay.model.card;

import org.junit.jupiter.api.Test;
import pp.winf2slay.model.effect.DrawCards;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardTest {

    @Test
    void cardsWithSameTypeAndNameAreEqual() {
        Hero a = new Hero("Mage_1", "Falck", 4, new DrawCards(2), ClassType.MAGE);
        Hero b = new Hero("Mage_1", "Falck", 4, new DrawCards(2), ClassType.MAGE);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void modificationsWithDifferentValuesAreNotEqual() {
        // Früher galten alle Modifikationen als gleich (Vergleich der ersten 11 Zeichen).
        assertNotEquals(new Modification(2), new Modification(-2));
        assertNotEquals(new Modification(2), new Modification(4));
        assertEquals(new Modification(-4), new Modification(-4));
    }

    @Test
    void challengesAreInterchangeable() {
        assertEquals(new Challenge(), new Challenge());
    }

    @Test
    void differentCardTypesWithSameNameAreNotEqual() {
        Spell spell = new Spell("X", "X", new DrawCards(1));
        Hero hero = new Hero("X", 3, new DrawCards(1), ClassType.BARD);
        assertNotEquals(spell, hero);
        Set<Card> set = new HashSet<>();
        set.add(spell);
        set.add(hero);
        assertEquals(2, set.size());
    }

    @Test
    void displayNameDefaultsToTechnicalName() {
        Hero hero = new Hero("Bard_8", 3, new DrawCards(1), ClassType.BARD);
        assertEquals("Bard_8", hero.getDisplayName());
        assertEquals("Modifikation +2", new Modification(2).getDisplayName());
        assertEquals("Modifikation_-4", new Modification(-4).getName());
    }

    @Test
    void monsterThresholdDirection() {
        Monster high = new Monster("M", "M", 8, true, null, null);
        assertTrue(high.isDefeatedBy(8));
        assertTrue(high.isDefeatedBy(12));
        assertFalse(high.isDefeatedBy(7));
        Monster low = new Monster("L", "L", 7, false, null, null);
        assertTrue(low.isDefeatedBy(7));
        assertTrue(low.isDefeatedBy(2));
        assertFalse(low.isDefeatedBy(8));
    }
}
