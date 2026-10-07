package pp.winf2slay.model.die;

import com.jme3.network.serializing.Serializable;

/**
 * Unveränderliches Ergebnis eines Wurfs mit zwei Würfeln.
 *
 * <p>Das Gesamtergebnis setzt sich zusammen aus den beiden Augenzahlen, dem
 * passiven Bonus des Spielers (Anführer, besiegte Monster) und der Summe aller
 * nachträglich gespielten Modifikationskarten.</p>
 */
@Serializable
public class DiceResult {

    private String playerName;
    private int die1;
    private int die2;
    private int bonus;
    private int modification;

    private DiceResult() {
        // für @Serializable
    }

    /**
     * @param playerName   Name des würfelnden Spielers
     * @param die1         Augenzahl des ersten Würfels (1–6)
     * @param die2         Augenzahl des zweiten Würfels (1–6)
     * @param bonus        passiver Bonus
     * @param modification Summe der gespielten Modifikationen
     */
    public DiceResult(String playerName, int die1, int die2, int bonus, int modification) {
        this.playerName = playerName;
        this.die1 = die1;
        this.die2 = die2;
        this.bonus = bonus;
        this.modification = modification;
    }

    /**
     * Liefert ein neues Ergebnis, auf das zusätzlich eine Modifikation angewendet wurde.
     *
     * @param delta Wert der Modifikationskarte
     * @return modifiziertes Ergebnis
     */
    public DiceResult modifiedBy(int delta) {
        return new DiceResult(playerName, die1, die2, bonus, modification + delta);
    }

    /**
     * @return Name des würfelnden Spielers
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * @return Augenzahl des ersten Würfels
     */
    public int getDie1() {
        return die1;
    }

    /**
     * @return Augenzahl des zweiten Würfels
     */
    public int getDie2() {
        return die2;
    }

    /**
     * @return passiver Bonus
     */
    public int getBonus() {
        return bonus;
    }

    /**
     * @return Summe der gespielten Modifikationen
     */
    public int getModification() {
        return modification;
    }

    /**
     * @return Summe aus Bonus und Modifikationen
     */
    public int getAdjustment() {
        return bonus + modification;
    }

    /**
     * @return Gesamtergebnis (Augen + Bonus + Modifikationen)
     */
    public int getTotal() {
        return die1 + die2 + bonus + modification;
    }

    @Override
    public String toString() {
        return playerName + ": " + die1 + "+" + die2 + (getAdjustment() >= 0 ? "+" : "") + getAdjustment() + "=" + getTotal();
    }
}
