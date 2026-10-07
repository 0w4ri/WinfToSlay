package pp.winf2slay.view.game.card;

import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Leader;
import pp.winf2slay.model.card.Monster;

/**
 * Kartenformate (Weltkoordinaten). Alle Karten haben das Seitenverhältnis 5:7
 * ihrer Bilder; Monster und Anführer sind etwas größer.
 */
public enum CardSize {
    /** Unterstützungskarten (Helden, Zauber, Modifikationen, Herausforderungen). */
    SMALL(6.0f, 8.4f),
    /** Monster und Anführer. */
    BIG(7.2f, 10.08f);

    /** Dicke einer Karte. */
    public static final float THICKNESS = 0.06f;

    private final float width;
    private final float height;

    CardSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    /**
     * @return Breite
     */
    public float getWidth() {
        return width;
    }

    /**
     * @return Höhe (Länge in Leserichtung)
     */
    public float getHeight() {
        return height;
    }

    /**
     * @param card Karte
     * @return passendes Format
     */
    public static CardSize of(Card card) {
        return card instanceof Monster || card instanceof Leader ? BIG : SMALL;
    }
}
