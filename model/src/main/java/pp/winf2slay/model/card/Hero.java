package pp.winf2slay.model.card;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.effect.Effect;

/**
 * Heldenkarte. Ein Held wird in die eigene Gruppe gelegt; sein Effekt wird
 * ausgelöst, wenn ein Wurf mit zwei Würfeln (plus Heldenbonus) mindestens den
 * {@linkplain #getThreshold() Schwellenwert} erreicht.
 */
@Serializable
public final class Hero extends Card {

    private int threshold;
    private Effect effect;
    private ClassType classType;

    /**
     * Konstruktor für die Netzwerkserialisierung.
     */
    Hero() {
        // für @Serializable
    }

    /**
     * Erzeugt einen Helden.
     *
     * @param name        technischer Name (Modell-Dateiname)
     * @param displayName Anzeigename
     * @param threshold   Mindestwurf, um den Effekt auszulösen
     * @param effect      Heldeneffekt
     * @param classType   Heldenklasse
     */
    public Hero(String name, String displayName, int threshold, Effect effect, ClassType classType) {
        super(name, displayName);
        this.threshold = threshold;
        this.effect = effect;
        this.classType = classType;
    }

    /**
     * Erzeugt einen Helden, dessen Anzeigename dem technischen Namen entspricht.
     *
     * @param name      technischer Name
     * @param threshold Mindestwurf
     * @param effect    Heldeneffekt
     * @param classType Heldenklasse
     */
    public Hero(String name, int threshold, Effect effect, ClassType classType) {
        this(name, null, threshold, effect, classType);
    }

    /**
     * @return Mindestwurf, um den Effekt auszulösen
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * @return Heldeneffekt
     */
    public Effect getEffect() {
        return effect;
    }

    /**
     * @return Heldenklasse
     */
    public ClassType getClassType() {
        return classType;
    }

    @Override
    public String getTypeName() {
        return "Held";
    }
}
