package pp.winf2slay.controller.server.flow;

import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.controller.message.client.CMModifySingleResponse;
import pp.winf2slay.controller.message.server.BCDiceModified;
import pp.winf2slay.controller.message.server.BCDiceRolled;
import pp.winf2slay.controller.message.server.SMModifySingleRequest;
import pp.winf2slay.controller.server.ServerGameLogic;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.die.DiceResult;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Würfelwurf eines Spielers mit anschließender Modifikationsrunde.
 *
 * <p>Ablauf:</p>
 * <ol>
 *     <li>Der Spieler würfelt (plus passivem Bonus), alle sehen das Ergebnis.</li>
 *     <li>Reihum werden alle Spieler mit Modifikationskarten gefragt, ob sie den
 *     Wurf verändern wollen. Spielt jemand eine Modifikation, beginnt die Runde
 *     von vorn – bis niemand mehr eingreifen möchte.</li>
 *     <li>Das Endergebnis wird an den Aufrufer zurückgegeben.</li>
 * </ol>
 */
public class DiceRollFlow {

    private final ServerGameLogic logic;
    private final RollPurpose purpose;
    private final Player roller;
    private final int threshold;
    private final boolean higherWins;
    private final int bonus;
    private DiceResult result;
    private Consumer<DiceResult> onDone;

    /**
     * @param logic      Serverlogik
     * @param purpose    Anlass des Wurfs
     * @param roller     würfelnder Spieler
     * @param threshold  zu erreichender Schwellenwert
     * @param higherWins {@code true}, wenn der Wurf den Schwellenwert erreichen muss
     * @param bonus      passiver Bonus des Spielers
     */
    public DiceRollFlow(ServerGameLogic logic, RollPurpose purpose, Player roller, int threshold,
                        boolean higherWins, int bonus) {
        this.logic = logic;
        this.purpose = purpose;
        this.roller = roller;
        this.threshold = threshold;
        this.higherWins = higherWins;
        this.bonus = bonus;
    }

    /**
     * Startet den Wurf.
     *
     * @param onDone erhält das endgültige Ergebnis
     */
    public void run(Consumer<DiceResult> onDone) {
        this.onDone = onDone;
        result = logic.getGame().roll(roller, bonus);
        logic.broadcast(new BCDiceRolled(result, purpose, threshold, higherWins), this::startModificationRound);
    }

    private void startModificationRound() {
        List<Player> candidates = Flows.playersWith(logic, Modification.class, roller);
        askNext(candidates.iterator());
    }

    private void askNext(Iterator<Player> candidates) {
        if (!candidates.hasNext()) {
            onDone.accept(result);
            return;
        }
        Player player = candidates.next();
        logic.request(player, new SMModifySingleRequest(result, purpose, threshold, higherWins),
                      CMModifySingleResponse.class, response -> {
                          Modification wanted = response.getModification();
                          if (wanted == null || !player.getHand().contains(wanted)) {
                              askNext(candidates);
                              return;
                          }
                          Modification mod = (Modification) logic.getGame().moveHandToDiscard(player, wanted);
                          result = result.modifiedBy(mod.getDelta());
                          logic.broadcast(new BCDiceModified(player.getName(), mod, result),
                                          this::startModificationRound);
                      });
    }
}
