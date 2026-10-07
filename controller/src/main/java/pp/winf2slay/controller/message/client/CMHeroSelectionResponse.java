package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Hero;

/**
 * Antwort auf eine Heldenauswahl (Effekt „Zerstöre einen Helden“).
 */
@Serializable
public class CMHeroSelectionResponse extends ClientMessage {

    private Hero hero;

    private CMHeroSelectionResponse() {
        // für @Serializable
    }

    /**
     * @param hero gewählter Held
     */
    public CMHeroSelectionResponse(Hero hero) {
        this.hero = hero;
    }

    /**
     * @return gewählter Held
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
        return "CMHeroSelectionResponse[" + "hero=" + hero + "]";
    }
}
