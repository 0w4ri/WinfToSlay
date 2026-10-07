package pp.winf2slay.model.card;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.effect.Effect;

/**
 * Monsterkarte. Monster liegen offen in der Tischmitte und können angegriffen
 * werden. Wer drei Monster besiegt, gewinnt.
 *
 * <p>Je nach Monster muss der Angriffswurf den Schwellenwert erreichen
 * ({@link #isHigherWins()} = {@code true}) oder darf ihn nicht überschreiten
 * ({@code false}).</p>
 */
@Serializable
public final class Monster extends Card {

    private int threshold;
    private boolean higherWins;
    private Effect penalty;
    private Effect reward;

    /**
     * Konstruktor für die Netzwerkserialisierung.
     */
    Monster() {
        // für @Serializable
    }

    /**
     * Erzeugt ein Monster.
     *
     * @param name        technischer Name (Modell-Dateiname)
     * @param displayName Anzeigename
     * @param threshold   Schwellenwert des Angriffswurfs
     * @param higherWins  {@code true}: Wurf ≥ Schwelle gewinnt; {@code false}: Wurf ≤ Schwelle gewinnt
     * @param penalty     Effekt bei einem verlorenen Kampf
     * @param reward      dauerhafter Bonus bei einem gewonnenen Kampf
     */
    public Monster(String name, String displayName, int threshold, boolean higherWins, Effect penalty, Effect reward) {
        super(name, displayName);
        this.threshold = threshold;
        this.higherWins = higherWins;
        this.penalty = penalty;
        this.reward = reward;
    }

    /**
     * Prüft, ob ein Angriffswurf das Monster besiegt.
     *
     * @param roll Gesamtergebnis des Angriffswurfs (inklusive Boni)
     * @return {@code true}, wenn das Monster besiegt ist
     */
    public boolean isDefeatedBy(int roll) {
        return higherWins ? roll >= threshold : roll <= threshold;
    }

    /**
     * @return Schwellenwert des Angriffswurfs
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * @return {@code true}, wenn der Wurf den Schwellenwert erreichen muss;
     *         {@code false}, wenn er ihn nicht überschreiten darf
     */
    public boolean isHigherWins() {
        return higherWins;
    }

    /**
     * @return Effekt bei einem verlorenen Kampf
     */
    public Effect getPenalty() {
        return penalty;
    }

    /**
     * @return Bonus bei einem gewonnenen Kampf
     */
    public Effect getReward() {
        return reward;
    }

    @Override
    public String getTypeName() {
        return "Monster";
    }
}
