package pp.winf2slay.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.ClassType;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Leader;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.deck.CardCatalog;
import pp.winf2slay.model.effect.AttackBonus;
import pp.winf2slay.model.effect.DrawCards;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {

    private Game game;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        game = new Game(13L);
        alice = new Player(0, "Alice");
        bob = new Player(1, "Bob");
        game.addPlayer(alice);
        game.addPlayer(bob);
    }

    /**
     * Zählt alle Karten im Spiel – es darf nie eine Karte verloren gehen.
     */
    private int totalSupportCards() {
        int total = game.getSupportDeck().size() + game.getDiscardPile().size();
        for (Player p : game.getPlayers()) {
            total += p.getHand().size() + p.getGroup().size() + (p.getHover().isEmpty() ? 0 : 1);
        }
        return total;
    }

    @Test
    void threeMonstersAreOpenAtStart() {
        assertEquals(3, game.getOpenMonsters().size());
        assertEquals(12, game.getMonsterDeck().size());
    }

    @Test
    void cardsAreNeverLost() {
        int start = totalSupportCards();
        for (int i = 0; i < 200; i++) {
            Player p = i % 2 == 0 ? alice : bob;
            game.drawSupportCard(p);
            if (p.getHand().size() > 6) {
                game.moveHandToDiscard(p, p.getHand().getCards().getFirst());
            }
        }
        game.mulligan(alice);
        assertEquals(start, totalSupportCards());
    }

    @Test
    void mulliganDiscardsHandAndDrawsFive() {
        for (int i = 0; i < 3; i++) game.drawSupportCard(alice);
        List<Card> before = List.copyOf(alice.getHand().getCards());
        Game.Mulligan result = game.mulligan(alice);
        assertEquals(before, result.discarded());
        assertEquals(Game.HAND_SIZE, result.drawn().size());
        assertEquals(Game.HAND_SIZE, alice.getHand().size());
    }

    @Test
    void playHeroThroughHoverIntoGroup() {
        Hero hero = CardCatalog.heroes().getFirst();
        alice.getHand().add(hero);
        game.moveHandToHover(alice, hero);
        assertFalse(alice.getHover().isEmpty());
        game.moveHoverToGroup(alice);
        assertTrue(alice.getGroup().contains(hero));
        assertTrue(alice.getHover().isEmpty());
        assertEquals(alice, game.findOwner(hero).orElseThrow());
        assertEquals(List.of(hero), game.getHeroesOfOpponents(bob));
        assertTrue(game.getHeroesOfOpponents(alice).isEmpty());
    }

    @Test
    void cannotPlayCardThatIsNotOnHand() {
        Hero hero = CardCatalog.heroes().getFirst();
        assertThrows(IllegalArgumentException.class, () -> game.moveHandToHover(alice, hero));
    }

    @Test
    void defeatingMonsterAppliesRewardAndRevealsNext() {
        Monster monster = game.getOpenMonsters().getMonsters().getFirst();
        int attackBefore = alice.getAttackBonus();
        boolean attackReward = monster.getReward() instanceof AttackBonus;
        game.defeatMonster(alice, monster);
        assertEquals(1, alice.getMonsters().size());
        assertEquals(3, game.getOpenMonsters().size());
        if (attackReward) assertEquals(attackBefore + 1, alice.getAttackBonus());
    }

    @Test
    void openMonstersRunOutGracefully() {
        // Mehr als 15 Siege sind nicht möglich – es darf aber auch nichts abstürzen.
        Player[] players = {alice, bob};
        int defeated = 0;
        while (game.getOpenMonsters().size() > 0) {
            Monster m = game.getOpenMonsters().getMonsters().getFirst();
            Player p = players[defeated % 2];
            if (p.getMonsters().hasFreeSlot()) game.defeatMonster(p, m);
            else game.defeatMonster(players[(defeated + 1) % 2], m);
            defeated++;
            if (defeated >= 6) break;
        }
        assertEquals(6, defeated);
    }

    @Test
    void winByClassesIncludingLeader() {
        Leader leader = new Leader("L", "L", new AttackBonus(1), ClassType.MAGE);
        alice.assignLeader(leader);
        assertEquals(1, alice.getAttackBonus());
        ClassType[] others = {ClassType.GUARD, ClassType.FIGHTER, ClassType.RANGER, ClassType.THIEF, ClassType.BARD};
        // Anführer + (CLASSES_TO_WIN - 2) weitere Klassen reichen noch nicht ...
        for (int i = 0; i < Game.CLASSES_TO_WIN - 2; i++) {
            alice.getGroup().add(new Hero(others[i].name(), 3, new DrawCards(1), others[i]));
        }
        // ... auch nicht mit einer doppelten Klasse ...
        alice.getGroup().add(new Hero("DUP", 3, new DrawCards(1), ClassType.MAGE));
        assertFalse(game.hasWon(alice));
        // ... erst mit einer weiteren neuen Klasse.
        ClassType last = others[Game.CLASSES_TO_WIN - 2];
        alice.getGroup().add(new Hero(last.name(), 3, new DrawCards(1), last));
        assertTrue(game.hasWon(alice));
    }

    @Test
    void winByThreeMonsters() {
        for (int i = 0; i < 2; i++) {
            game.defeatMonster(bob, game.getOpenMonsters().getMonsters().getFirst());
        }
        assertFalse(game.hasWon(bob));
        game.defeatMonster(bob, game.getOpenMonsters().getMonsters().getFirst());
        assertTrue(game.hasWon(bob));
    }

    @Test
    void atMostSixPlayers() {
        for (int i = 2; i < Game.MAX_PLAYERS; i++) game.addPlayer(new Player(i, "P" + i));
        assertThrows(IllegalStateException.class, () -> game.addPlayer(new Player(99, "zu viel")));
    }
}
