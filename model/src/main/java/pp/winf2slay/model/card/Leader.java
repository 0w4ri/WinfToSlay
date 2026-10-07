package pp.winf2slay.model.card;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.effect.Effect;

/**
 * Anführerkarte. Jeder Spieler erhält zu Beginn zufällig einen Anführer. Dessen
 * passiver Effekt (ein Würfelbonus) gilt das ganze Spiel über, und seine Klasse
 * zählt für die Siegbedingung „sechs Klassen“ mit.
 */
@Serializable
public final class Leader extends Card {

    private Effect effect;
    private ClassType classType;

    /**
     * Konstruktor für die Netzwerkserialisierung.
     */
    Leader() {
        // für @Serializable
    }

    /**
     * Erzeugt einen Anführer.
     *
     * @param name        technischer Name (Modell-Dateiname)
     * @param displayName Anzeigename
     * @param effect      passiver Effekt
     * @param classType   Klasse des Anführers
     */
    public Leader(String name, String displayName, Effect effect, ClassType classType) {
        super(name, displayName);
        this.effect = effect;
        this.classType = classType;
    }

    /**
     * @return passiver Effekt des Anführers
     */
    public Effect getEffect() {
        return effect;
    }

    /**
     * @return Klasse des Anführers
     */
    public ClassType getClassType() {
        return classType;
    }

    @Override
    public String getTypeName() {
        return "Anführer";
    }
}
