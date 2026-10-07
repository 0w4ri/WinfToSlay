package pp.winf2slay.model;

import org.junit.jupiter.api.Test;
import pp.winf2slay.model.card.ClassType;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Leader;
import pp.winf2slay.model.deck.CardCatalog;
import pp.winf2slay.model.effect.AttackBonus;
import pp.winf2slay.model.effect.ChallengeBonus;
import pp.winf2slay.model.effect.DrawCards;
import pp.winf2slay.model.effect.HeroBonus;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTest {

    @Test
    void newPlayerStartsEmpty() {
        Player p = new Player(3, "Anna");
        assertEquals(3, p.getId());
        assertEquals("Anna", p.getName());
        assertFalse(p.isBot());
        assertNull(p.getLeader());
        assertEquals(0, p.getHand().size());
        assertEquals(0, p.getGroup().size());
        assertTrue(p.getHover().isEmpty());
        assertEquals(0, p.getAttackBonus() + p.getChallengeBonus() + p.getHeroBonus());
        assertTrue(p.getClassTypes().isEmpty());
    }

    @Test
    void replaceByBotKeepsIdentity() {
        Player p = new Player(1, "Ben");
        p.replaceByBot();
        assertTrue(p.isBot());
        assertEquals("Ben", p.getName());
        assertEquals(1, p.getId());
    }

    @Test
    void leaderAppliesItsPassiveBonusOnce() {
        Player attack = new Player(0, "A");
        attack.assignLeader(new Leader("LA", "LA", new AttackBonus(2), ClassType.FIGHTER));
        assertEquals(2, attack.getAttackBonus());

        Player challenge = new Player(1, "C");
        challenge.assignLeader(new Leader("LC", "LC", new ChallengeBonus(1), ClassType.BARD));
        assertEquals(1, challenge.getChallengeBonus());

        Player hero = new Player(2, "H");
        hero.assignLeader(new Leader("LH", "LH", new HeroBonus(1), ClassType.MAGE));
        assertEquals(1, hero.getHeroBonus());

        Leader second = new Leader("L2", "L2", new AttackBonus(1), ClassType.THIEF);
        assertThrows(IllegalStateException.class, () -> attack.assignLeader(second));
        assertEquals(2, attack.getAttackBonus(), "Ein zweiter Anführer darf nichts verändern");
    }

    @Test
    void everyCatalogLeaderCanBeAssigned() {
        int i = 0;
        for (Leader leader : CardCatalog.leaders()) {
            Player p = new Player(i++, "P" + i);
            p.assignLeader(leader);
            assertTrue(p.getClassTypes().contains(leader.getClassType()));
        }
    }

    @Test
    void bonusesAccumulateAndRejectNegativeValues() {
        Player p = new Player(0, "A");
        p.addAttackBonus(1);
        p.addAttackBonus(2);
        p.addChallengeBonus(1);
        p.addHeroBonus(3);
        assertEquals(3, p.getAttackBonus());
        assertEquals(1, p.getChallengeBonus());
        assertEquals(3, p.getHeroBonus());
        assertThrows(IllegalArgumentException.class, () -> p.addAttackBonus(-1));
        assertThrows(IllegalArgumentException.class, () -> p.addChallengeBonus(-1));
        assertThrows(IllegalArgumentException.class, () -> p.addHeroBonus(-1));
    }

    @Test
    void classTypesCombineLeaderAndGroupWithoutDuplicates() {
        Player p = new Player(0, "A");
        p.assignLeader(new Leader("L", "L", new AttackBonus(1), ClassType.MAGE));
        p.getGroup().add(new Hero("H1", 5, new DrawCards(1), ClassType.MAGE));
        p.getGroup().add(new Hero("H2", 5, new DrawCards(1), ClassType.RANGER));
        p.getGroup().add(new Hero("H3", 5, new DrawCards(1), ClassType.RANGER));
        assertEquals(EnumSet.of(ClassType.MAGE, ClassType.RANGER), p.getClassTypes());
    }
}
