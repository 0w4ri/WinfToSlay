package pp.winf2slay.model.deck;

import org.junit.jupiter.api.Test;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Challenge;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.card.Spell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckTest {

    @Test
    void sameSeedGivesSameOrder() {
        Deck<Card> a = CardCatalog.createSupportDeck(new Random(42));
        Deck<Card> b = CardCatalog.createSupportDeck(new Random(42));
        assertEquals(a.snapshot(), b.snapshot());
    }

    @Test
    void drawRemovesFromTopAndEmptyDeckReturnsEmpty() {
        Deck<Card> deck = new Deck<>(List.of(new Modification(2), new Modification(4)), new Random(1));
        Card first = deck.peek().orElseThrow();
        assertEquals(first, deck.draw().orElseThrow());
        assertEquals(1, deck.size());
        deck.draw();
        assertTrue(deck.isEmpty());
        assertTrue(deck.draw().isEmpty());
        assertTrue(deck.draw(3).isEmpty());
    }

    @Test
    void shuffleUnderMovesWholeDiscardPile() {
        Deck<Card> deck = new Deck<>(List.of(new Challenge()), new Random(1));
        List<Card> discard = new ArrayList<>(List.of(new Modification(2), new Modification(-2)));
        deck.shuffleUnder(discard);
        assertTrue(discard.isEmpty());
        assertEquals(3, deck.size());
        assertEquals(new Challenge(), deck.draw().orElseThrow());
    }

    @Test
    void catalogHasExpectedComposition() {
        List<Card> support = CardCatalog.supportCards();
        assertEquals(48, support.stream().filter(c -> c instanceof Hero).count());
        assertEquals(7, support.stream().filter(c -> c instanceof Spell).count());
        assertEquals(16, support.stream().filter(c -> c instanceof Modification).count());
        assertEquals(20, support.stream().filter(c -> c instanceof Challenge).count());
        assertEquals(15, CardCatalog.monsters().size());
        assertEquals(6, CardCatalog.leaders().size());
        // Heldennamen sind eindeutig (wichtig für die Zuordnung zu 3D-Modellen)
        assertEquals(48, new HashSet<>(CardCatalog.heroes()).size());
    }
}
