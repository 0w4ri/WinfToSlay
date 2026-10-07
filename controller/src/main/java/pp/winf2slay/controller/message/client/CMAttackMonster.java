package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Monster;

/**
 * Aktion: ein offenes Monster angreifen (2 Aktionspunkte, mindestens zwei Helden).
 */
@Serializable
public class CMAttackMonster extends ClientMessage {

    private Monster monster;

    private CMAttackMonster() {
        // für @Serializable
    }

    /**
     * @param monster angegriffenes Monster
     */
    public CMAttackMonster(Monster monster) {
        this.monster = monster;
    }

    /**
     * @return angegriffenes Monster
     */
    public Monster getMonster() {
        return monster;
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMAttackMonster[" + "monster=" + monster + "]";
    }
}
