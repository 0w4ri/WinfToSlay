package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Hero;

/**
 * Aktion: den Effekt eines eigenen Helden auslösen (1 Aktionspunkt).
 */
@Serializable
public class CMActivateHeroEffect extends ClientMessage {

    private Hero hero;

    private CMActivateHeroEffect() {
        // für @Serializable
    }

    /**
     * @param hero Held aus der eigenen Gruppe
     */
    public CMActivateHeroEffect(Hero hero) {
        this.hero = hero;
    }

    /**
     * @return Held aus der eigenen Gruppe
     */
    public Hero getHero() {
        return hero;
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMActivateHeroEffect[" + "hero=" + hero + "]";
    }
}
