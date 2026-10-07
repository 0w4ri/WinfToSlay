package pp.winf2slay.model.effect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.ClassType;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Modification;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectTest {

    private Game game;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        game = new Game(5L);
        alice = new Player(0, "Alice");
        bob = new Player(1, "Bob");
        game.addPlayer(alice);
        game.addPlayer(bob);
    }

    private Hero give(Player player, String name) {
        Hero hero = new Hero(name, 5, new DrawCards(1), ClassType.FIGHTER);
        player.getGroup().add(hero);
        return hero;
    }

    @Test
    void passiveBonuses() {
        new AttackBonus(1).applyTo(alice);
        new ChallengeBonus(2).applyTo(alice);
        new HeroBonus(3).applyTo(alice);
        assertEquals(1, alice.getAttackBonus());
        assertEquals(2, alice.getChallengeBonus());
        assertEquals(3, alice.getHeroBonus());
    }

    @Test
    void drawCardsDrawsOneCardPerCall() {
        DrawCards effect = new DrawCards(2);
        assertEquals(2, effect.getCount());
        assertTrue(effect.drawOne(game, alice).isPresent());
        assertEquals(1, alice.getHand().size());
    }

    @Test
    void drawFromDiscardTakesTopCard() {
        DrawFromDiscard effect = new DrawFromDiscard(1);
        assertTrue(effect.takeOne(game, alice).isEmpty());
        bob.getHand().add(new Modification(4));
        game.moveHandToDiscard(bob, new Modification(4));
        assertEquals(new Modification(4), effect.takeOne(game, alice).orElseThrow());
        assertTrue(game.getDiscardPile().isEmpty());
    }

    @Test
    void destroyHeroTargetsOnlyOpponents() {
        give(alice, "Own");
        Hero enemy = give(bob, "Enemy");
        DestroyHero effect = new DestroyHero(1);
        assertEquals(List.of(enemy), effect.targetsFor(game, alice));
        assertEquals(bob, effect.destroy(game, enemy).orElseThrow());
        assertTrue(bob.getGroup().isEmpty());
        assertEquals(enemy, game.getDiscardPile().getLast());
        assertTrue(effect.destroy(game, enemy).isEmpty(), "zweites Zerstören darf nichts tun");
    }

    @Test
    void destroyAllHeroesHitsEveryone() {
        give(alice, "A1");
        give(bob, "B1");
        give(bob, "B2");
        Map<Player, List<Hero>> destroyed = new DestroyAllHeroes().destroyAll(game);
        assertEquals(1, destroyed.get(alice).size());
        assertEquals(2, destroyed.get(bob).size());
        assertTrue(alice.getGroup().isEmpty());
        assertTrue(bob.getGroup().isEmpty());
    }

    @Test
    void destroyPartyOnlyHitsTarget() {
        give(alice, "A1");
        give(bob, "B1");
        DestroyParty effect = new DestroyParty();
        assertEquals(List.of(alice, bob), effect.targets(game));
        assertEquals(1, effect.destroyParty(game, bob).size());
        assertFalse(alice.getGroup().isEmpty());
        assertEquals(List.of(alice), effect.targets(game));
    }

    @Test
    void destroyRandomAndSacrifice() {
        assertTrue(new DestroyRandomHeroes(2).destroyOne(game).isEmpty());
        assertTrue(new SacrificeHero().sacrifice(game, alice).isEmpty());
        Hero h = give(bob, "B1");
        DestroyRandomHeroes.Destroyed d = new DestroyRandomHeroes(2).destroyOne(game).orElseThrow();
        assertEquals(h, d.hero());
        assertEquals(bob, d.owner());
        give(alice, "A1");
        assertTrue(new SacrificeHero().sacrifice(game, alice).isPresent());
        assertTrue(alice.getGroup().isEmpty());
    }

    @Test
    void everyoneDiscardsOneCardEach() {
        alice.getHand().add(new Modification(2));
        alice.getHand().add(new Modification(4));
        Map<Player, Card> discarded = new EveryoneDiscards(2).discardOneEach(game);
        assertEquals(1, discarded.size(), "Bob hat keine Karten");
        assertEquals(1, alice.getHand().size());
        assertEquals(1, game.getDiscardPile().size());
    }

    @Test
    void descriptionsAreGerman() {
        assertEquals("Ziehe 2 Karten.", new DrawCards(2).describe());
        assertEquals("Zerstöre einen ausgewählten Helden.", new DestroyHero(1).describe());
        assertEquals("Opfere einen Helden.", new SacrificeHero().describe());
    }
}
