package pp.winf2slay.model.card;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.effect.Effect;

/**
 * Zauberkarte. Ein Zauber wird ausgespielt, kann herausgefordert werden und
 * entfaltet danach sofort seinen Effekt. Anschließend landet er auf dem
 * Ablagestapel.
 */
@Serializable
public final class Spell extends Card {

    private Effect effect;

    /**
     * Konstruktor für die Netzwerkserialisierung.
     */
    Spell() {
        // für @Serializable
    }

    /**
     * Erzeugt einen Zauber.
     *
     * @param name        technischer Name (Modell-Dateiname)
     * @param displayName Anzeigename
     * @param effect      Effekt des Zaubers
     */
    public Spell(String name, String displayName, Effect effect) {
        super(name, displayName);
        this.effect = effect;
    }

    /**
     * @return Effekt des Zaubers
     */
    public Effect getEffect() {
        return effect;
    }

    @Override
    public String getTypeName() {
        return "Zauber";
    }
}
