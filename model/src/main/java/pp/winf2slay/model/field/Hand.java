package pp.winf2slay.model.field;

import pp.winf2slay.model.card.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handkarten eines Spielers.
 */
public class Hand {

    private final List<Card> cards = new ArrayList<>();

    /**
     * @return unveränderliche Sicht auf die Handkarten
     */
    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    /**
     * @return Anzahl der Handkarten
     */
    public int size() {
        return cards.size();
    }

    /**
     * @return {@code true}, wenn keine Karten auf der Hand sind
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * Nimmt eine Karte auf die Hand.
     *
     * @param card Karte (nicht {@code null})
     */
    public void add(Card card) {
        cards.add(Objects.requireNonNull(card, "card"));
    }

    /**
     * Prüft, ob eine gleichwertige Karte auf der Hand ist.
     *
     * @param card gesuchte Karte
     * @return {@code true}, wenn vorhanden
     */
    public boolean contains(Card card) {
        return cards.contains(card);
    }

    /**
     * Entfernt ein Exemplar der angegebenen Karte und liefert die tatsächlich
     * entfernte Instanz. So arbeitet der Server immer mit seinen eigenen
     * Kartenobjekten, auch wenn ein Client nur eine Kopie geschickt hat.
     *
     * @param card zu entfernende Karte
     * @return entfernte Instanz; leer, wenn die Karte nicht auf der Hand ist
     */
    public Optional<Card> take(Card card) {
        int index = cards.indexOf(card);
        return index < 0 ? Optional.empty() : Optional.of(cards.remove(index));
    }

    /**
     * Entfernt alle Handkarten.
     *
     * @return die bisherigen Handkarten
     */
    public List<Card> clear() {
        List<Card> old = new ArrayList<>(cards);
        cards.clear();
        return old;
    }
}
