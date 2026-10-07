package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Monster;

/**
 * Ein Spieler greift ein Monster an.
 *
 * <p>Broadcast-Nachricht: Jeder Client bestätigt sie nach den Animationen.</p>
 */
@Serializable
public class BCMonsterAttacked extends BroadcastMessage {

    private String playerName;
    private Monster monster;

    private BCMonsterAttacked() {
        // für @Serializable
    }

    /**
     * @param playerName Angreifer
     * @param monster angegriffenes Monster
     */
    public BCMonsterAttacked(String playerName, Monster monster) {
        this.playerName = playerName;
        this.monster = monster;
    }

    /**
     * @return Angreifer
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * @return angegriffenes Monster
     */
    public Monster getMonster() {
        return monster;
    }

    @Override
    public void accept(ServerMessageInterpreter interpreter) {
        interpreter.received(this);
    }

    @Override
    public String toString() {
        return "BCMonsterAttacked[" + "playerName=" + playerName + ", " + "monster=" + monster + "]";
    }
}
