package pp.winf2slay.model.field;

import org.junit.jupiter.api.Test;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.ClassType;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.effect.DrawCards;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldTest {

    private static Hero hero(String name, ClassType type) {
        return new Hero(name, 5, new DrawCards(1), type);
    }

    @Test
    void groupKeepsSlotPositions() {
        Group group = new Group();
        Hero a = hero("A", ClassType.MAGE);
        Hero b = hero("B", ClassType.BARD);
        Hero c = hero("C", ClassType.BARD);
        assertTrue(group.add(a));
        assertTrue(group.add(b));
        assertTrue(group.remove(a));
        assertNull(group.getSlots()[0]);
        assertSame(b, group.getSlots()[1]);
        group.add(c);
        assertSame(c, group.getSlots()[0]);
        assertEquals(Set.of(ClassType.BARD), group.getClassTypes());
    }

    @Test
    void groupIsLimitedToFiveHeroes() {
        Group group = new Group();
        for (int i = 0; i < Group.SIZE; i++) {
            assertTrue(group.add(hero("H" + i, ClassType.THIEF)));
        }
        assertFalse(group.hasFreeSlot());
        assertFalse(group.add(hero("X", ClassType.THIEF)));
        assertFalse(group.remove(hero("unbekannt", ClassType.THIEF)));
    }

    @Test
    void handTakeReturnsOwnInstance() {
        Hand hand = new Hand();
        Modification own = new Modification(2);
        hand.add(own);
        Card copy = new Modification(2);
        assertSame(own, hand.take(copy).orElseThrow());
        assertTrue(hand.isEmpty());
        assertTrue(hand.take(copy).isEmpty());
    }

    @Test
    void hoverIsEmptyReportsCorrectly() {
        // Früher lieferte isEmpty() das Gegenteil.
        Hover hover = new Hover();
        assertTrue(hover.isEmpty());
        hover.put(new Modification(2));
        assertFalse(hover.isEmpty());
        assertThrows(IllegalStateException.class, () -> hover.put(new Modification(4)));
        assertEquals(new Modification(2), hover.take().orElseThrow());
        assertTrue(hover.isEmpty());
    }

    @Test
    void monsterFieldRemoveWithFreeSlotDoesNotFail() {
        // Früher NullPointerException, wenn ein Platz leer war.
        MonsterField field = new MonsterField();
        Monster m = new Monster("M", "M", 8, true, null, null);
        field.add(m);
        assertFalse(field.remove(new Monster("X", "X", 8, true, null, null)));
        assertTrue(field.remove(m));
        assertEquals(0, field.size());
    }
}
