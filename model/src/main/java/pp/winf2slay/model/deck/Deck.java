package pp.winf2slay.model.deck;

import pp.winf2slay.model.card.Card;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Verdeckter Kartenstapel, der beim Erzeugen gemischt wird.
 *
 * <p>Das Mischen verwendet den übergebenen Zufallsgenerator; mit einem festen
 * Seed ist die Reihenfolge reproduzierbar.</p>
 *
 * @param <T> Kartentyp
 */
public class Deck<T extends Card> {

    private final Deque<T> cards;
    private final Random rng;

    /**
     * Erzeugt einen gemischten Stapel.
     *
     * @param initialCards Karten des Stapels
     * @param rng          Zufallsgenerator zum Mischen
     */
    public Deck(Collection<? extends T> initialCards, Random rng) {
        Objects.requireNonNull(initialCards, "initialCards");
        this.rng = Objects.requireNonNull(rng, "rng");
        List<T> tmp = new ArrayList<>(initialCards);
        Collections.shuffle(tmp, this.rng);
        this.cards = new ArrayDeque<>(tmp);
    }

    /**
     * @return Anzahl der Karten
     */
    public int size() {
        return cards.size();
    }

    /**
     * @return {@code true}, wenn der Stapel leer ist
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * Zieht die oberste Karte.
     *
     * @return gezogene Karte; leer, wenn der Stapel leer ist
     */
    public Optional<T> draw() {
        return Optional.ofNullable(cards.pollFirst());
    }

    /**
     * Zieht bis zu {@code count} Karten.
     *
     * @param count gewünschte Anzahl
     * @return gezogene Karten (ggf. weniger als gewünscht)
     */
    public List<T> draw(int count) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < count && !cards.isEmpty(); i++) {
            result.add(cards.pollFirst());
        }
        return result;
    }

    /**
     * @return oberste Karte, ohne sie zu ziehen; leer, wenn der Stapel leer ist
     */
    public Optional<T> peek() {
        return Optional.ofNullable(cards.peekFirst());
    }

    /**
     * Legt eine Karte oben auf den Stapel.
     *
     * @param card Karte
     */
    public void putOnTop(T card) {
        cards.addFirst(Objects.requireNonNull(card, "card"));
    }

    /**
     * Legt eine Karte unter den Stapel.
     *
     * @param card Karte
     */
    public void putToBottom(T card) {
        cards.addLast(Objects.requireNonNull(card, "card"));
    }

    /**
     * Nimmt die erste Karte aus dem Stapel, die die Bedingung erfüllt (für
     * Entwickler-Testaufbauten).
     *
     * @param filter Bedingung
     * @return Karte oder leer
     */
    public Optional<T> takeFirst(Predicate<? super T> filter) {
        for (T card : cards) {
            if (filter.test(card)) {
                cards.remove(card);
                return Optional.of(card);
            }
        }
        return Optional.empty();
    }

    /**
     * @return Kopie der Karten in aktueller Reihenfolge (oberste zuerst)
     */
    public List<T> snapshot() {
        return new ArrayList<>(cards);
    }

    /**
     * Mischt die Karten eines Ablagestapels und legt sie unter diesen Stapel.
     * Der Ablagestapel ist danach leer.
     *
     * @param discardPile Ablagestapel
     */
    public void shuffleUnder(List<? extends T> discardPile) {
        if (discardPile.isEmpty()) return;
        List<T> tmp = new ArrayList<>(discardPile);
        Collections.shuffle(tmp, rng);
        cards.addAll(tmp);
        discardPile.clear();
    }
}
