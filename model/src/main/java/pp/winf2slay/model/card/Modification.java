package pp.winf2slay.model.card;

import com.jme3.network.serializing.Serializable;

/**
 * Modifikationskarte. Sie kann nach einem beliebigen Würfelwurf gespielt werden
 * und verändert dessen Ergebnis um {@link #getDelta()}.
 */
@Serializable
public final class Modification extends Card {

    private int delta;

    /**
     * Konstruktor für die Netzwerkserialisierung.
     */
    Modification() {
        // für @Serializable
    }

    /**
     * Erzeugt eine Modifikation. Der technische Name lautet
     * {@code Modifikation_<delta>} (z. B. {@code Modifikation_-2}).
     *
     * @param delta Wert, um den ein Wurf verändert wird (positiv oder negativ)
     */
    public Modification(int delta) {
        super("Modifikation_" + delta, "Modifikation " + (delta > 0 ? "+" : "") + delta);
        this.delta = delta;
    }

    /**
     * @return Wert, um den ein Wurf verändert wird
     */
    public int getDelta() {
        return delta;
    }

    @Override
    public String getTypeName() {
        return "Modifikation";
    }
}
