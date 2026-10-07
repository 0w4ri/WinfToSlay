package pp.winf2slay.model.card;

import com.jme3.network.serializing.Serializable;

/**
 * Herausforderungskarte. Wird ein Held oder Zauber ausgespielt, dürfen die
 * anderen Spieler ihn mit einer Herausforderung anfechten. Beide Seiten würfeln;
 * verliert der aktive Spieler, wird die ausgespielte Karte abgeworfen.
 */
@Serializable
public final class Challenge extends Card {

    /**
     * Technischer Name aller Herausforderungen.
     */
    public static final String NAME = "Herausforderung";

    /**
     * Erzeugt eine Herausforderung.
     */
    public Challenge() {
        super(NAME, NAME);
    }

    @Override
    public String getTypeName() {
        return "Herausforderung";
    }
}
