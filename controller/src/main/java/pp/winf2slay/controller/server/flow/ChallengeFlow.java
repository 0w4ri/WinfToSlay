package pp.winf2slay.controller.server.flow;

import pp.winf2slay.controller.message.CardPosition;
import pp.winf2slay.controller.message.MoveCause;
import pp.winf2slay.controller.message.client.CMChallengeResponse;
import pp.winf2slay.controller.message.client.CMModifyDoubleResponse;
import pp.winf2slay.controller.message.server.BCCardMoved;
import pp.winf2slay.controller.message.server.BCChallengeModified;
import pp.winf2slay.controller.message.server.BCChallengeResolved;
import pp.winf2slay.controller.message.server.BCChallengeRolled;
import pp.winf2slay.controller.message.server.SMChallengeRequest;
import pp.winf2slay.controller.message.server.SMModifyDoubleRequest;
import pp.winf2slay.controller.server.ServerGameLogic;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Challenge;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.die.DiceResult;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Herausforderungsrunde für eine ausgespielte Karte.
 *
 * <p>Ablauf:</p>
 * <ol>
 *     <li>Reihum werden alle Gegner mit einer Herausforderungskarte gefragt.
 *     Der erste, der herausfordert, wird zum Herausforderer.</li>
 *     <li>Beide würfeln (plus Herausforderungsbonus).</li>
 *     <li>Alle Spieler mit Modifikationen dürfen einen der beiden Würfe
 *     verändern; nach jeder Modifikation beginnt die Runde von vorn.</li>
 *     <li>Der aktive Spieler gewinnt nur mit einem <em>höheren</em> Ergebnis.</li>
 * </ol>
 */
public class ChallengeFlow {

    private final ServerGameLogic logic;
    private final Player active;
    private final Card card;
    private Player challenger;
    private DiceResult activeResult;
    private DiceResult challengerResult;
    private Consumer<Boolean> onDone;

    /**
     * @param logic  Serverlogik
     * @param active Spieler, der die Karte ausgespielt hat
     * @param card   ausgespielte Karte
     */
    public ChallengeFlow(ServerGameLogic logic, Player active, Card card) {
        this.logic = logic;
        this.active = active;
        this.card = card;
    }

    /**
     * Startet die Herausforderungsrunde.
     *
     * @param onDone erhält {@code true}, wenn die Karte bestehen bleibt
     */
    public void run(Consumer<Boolean> onDone) {
        this.onDone = onDone;
        List<Player> candidates = Flows.playersWith(logic, Challenge.class, active);
        candidates.remove(active);
        askForChallenge(candidates.iterator());
    }

    private void askForChallenge(Iterator<Player> candidates) {
        if (!candidates.hasNext()) {
            onDone.accept(true);
            return;
        }
        Player player = candidates.next();
        logic.request(player, new SMChallengeRequest(active.getName(), card), CMChallengeResponse.class, response -> {
            Challenge wanted = response.getChallenge();
            if (wanted == null || !player.getHand().contains(wanted)) {
                askForChallenge(candidates);
                return;
            }
            challenger = player;
            Card played = logic.getGame().moveHandToDiscard(player, wanted);
            logic.broadcast(new BCCardMoved(player.getName(), player.getName(), played, CardPosition.PLAYER_HAND,
                                            CardPosition.DISCARD_PILE, MoveCause.CHALLENGE), this::rollBoth);
        });
    }

    private void rollBoth() {
        activeResult = logic.getGame().roll(active, active.getChallengeBonus());
        challengerResult = logic.getGame().roll(challenger, challenger.getChallengeBonus());
        logic.broadcast(new BCChallengeRolled(active.getName(), activeResult, challenger.getName(), challengerResult),
                        this::startModificationRound);
    }

    private void startModificationRound() {
        askForModification(Flows.playersWith(logic, Modification.class, active).iterator());
    }

    private void askForModification(Iterator<Player> candidates) {
        if (!candidates.hasNext()) {
            resolve();
            return;
        }
        Player player = candidates.next();
        logic.request(player, new SMModifyDoubleRequest(activeResult, challengerResult), CMModifyDoubleResponse.class,
                      response -> {
                          Modification wanted = response.getModification();
                          String target = response.getTargetPlayer();
                          boolean validTarget = active.getName().equals(target) || challenger.getName().equals(target);
                          if (wanted == null || !validTarget || !player.getHand().contains(wanted)) {
                              askForModification(candidates);
                              return;
                          }
                          Modification mod = (Modification) logic.getGame().moveHandToDiscard(player, wanted);
                          if (active.getName().equals(target))
                              activeResult = activeResult.modifiedBy(mod.getDelta());
                          else
                              challengerResult = challengerResult.modifiedBy(mod.getDelta());
                          logic.broadcast(new BCChallengeModified(player.getName(), mod, target, activeResult,
                                                                  challengerResult), this::startModificationRound);
                      });
    }

    private void resolve() {
        boolean activeWon = activeResult.getTotal() > challengerResult.getTotal();
        logic.broadcast(new BCChallengeResolved(active.getName(), challenger.getName(), activeWon),
                        () -> onDone.accept(activeWon));
    }
}
