package pp.winf2slay.controller.server.bot;

import pp.winf2slay.controller.message.BotLevel;
import pp.winf2slay.controller.message.server.ServerMessage;
import pp.winf2slay.controller.server.ServerGameLogic;
import pp.winf2slay.model.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verwaltet alle Bots eines Spiels.
 *
 * <p>Bots sind serverseitige Mitspieler: Nachrichten an sie werden nicht über
 * das Netzwerk verschickt, sondern direkt an ihren {@link BotController}
 * übergeben. Ihre Antworten laufen – wie die eines Menschen – über
 * {@link ServerGameLogic#receive}.</p>
 */
public class BotManager {

    private static final List<String> NAMES = List.of(
            "Bot Zuse", "Bot Lovelace", "Bot Turing", "Bot Hopper", "Bot Knuth", "Bot Babbage", "Bot Hamilton");

    private final ServerGameLogic logic;
    private final Map<Integer, BotController> controllers = new HashMap<>();
    private int nameIndex;
    private volatile double speedFactor = 1.0;

    /**
     * @param logic Serverlogik
     */
    public BotManager(ServerGameLogic logic) {
        this.logic = logic;
    }

    /**
     * @return nächster freier Bot-Name
     */
    public String nextName() {
        for (int i = 0; i < NAMES.size(); i++) {
            String candidate = NAMES.get((nameIndex + i) % NAMES.size());
            if (logic.findPlayer(candidate).isEmpty()) {
                nameIndex = (nameIndex + i + 1) % NAMES.size();
                return candidate;
            }
        }
        return "Bot";
    }

    /**
     * Meldet einen Bot an.
     *
     * @param player Spieler, der vom Bot gesteuert wird
     * @param level  Spielstärke
     */
    public void register(Player player, BotLevel level) {
        controllers.put(player.getId(), new BotController(logic, player, BotStrategy.forLevel(level)));
    }

    /**
     * Meldet einen Bot ab.
     *
     * @param player Spieler
     */
    public void unregister(Player player) {
        controllers.remove(player.getId());
    }

    /**
     * @param player Spieler
     * @return Spielstärke des Bots oder {@code null}, wenn der Spieler kein Bot ist
     */
    public BotLevel levelOf(Player player) {
        BotController controller = controllers.get(player.getId());
        return controller == null ? null : controller.getLevel();
    }

    /**
     * Übergibt eine Nachricht an den Bot eines Spielers.
     *
     * @param player Bot-Spieler
     * @param msg    Nachricht
     */
    public void deliver(Player player, ServerMessage msg) {
        BotController controller = controllers.get(player.getId());
        if (controller != null) controller.onMessage(msg);
    }

    /**
     * Legt fest, wie schnell Bots handeln (1 = normal, 0 = ohne Wartezeit).
     *
     * @param speedFactor Faktor für alle Bedenkzeiten
     */
    public void setSpeedFactor(double speedFactor) {
        this.speedFactor = Math.max(0, speedFactor);
    }

    /**
     * @return Faktor für alle Bedenkzeiten
     */
    double getSpeedFactor() {
        return speedFactor;
    }
}
