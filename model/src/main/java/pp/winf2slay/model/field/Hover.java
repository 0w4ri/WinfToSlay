package pp.winf2slay.model.field;

import pp.winf2slay.model.card.Card;

import java.util.Objects;
import java.util.Optional;

/**
 * Aktionsfeld („Hover“) eines Spielers.
 *
 * <p>Eine gerade ausgespielte Karte schwebt hier, bis feststeht, ob sie
 * herausgefordert wird. Danach wandert sie in die Gruppe (Held) oder auf den
 * Ablagestapel (Zauber oder verlorene Herausforderung).</p>
 */
public class Hover {

    private Card card;

    /**
     * Legt eine Karte in das Aktionsfeld.
     *
     * @param card Karte (nicht {@code null})
     * @throws IllegalStateException wenn bereits eine Karte im Aktionsfeld liegt
     */
    public void put(Card card) {
        Objects.requireNonNull(card, "card");
        if (this.card != null)
            throw new IllegalStateException("Aktionsfeld ist bereits belegt: " + this.card);
        this.card = card;
    }

    /**
     * Entfernt die Karte aus dem Aktionsfeld.
     *
     * @return entfernte Karte; leer, wenn das Feld leer war
     */
    public Optional<Card> take() {
        Card tmp = card;
        card = null;
        return Optional.ofNullable(tmp);
    }

    /**
     * @return Karte im Aktionsfeld; leer, wenn das Feld leer ist
     */
    public Optional<Card> getCard() {
        return Optional.ofNullable(card);
    }

    /**
     * @return {@code true}, wenn keine Karte im Aktionsfeld liegt
     */
    public boolean isEmpty() {
        return card == null;
    }
}
