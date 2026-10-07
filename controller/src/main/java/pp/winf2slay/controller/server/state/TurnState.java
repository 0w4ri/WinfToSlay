package pp.winf2slay.controller.server.state;

import pp.winf2slay.controller.message.CardPosition;
import pp.winf2slay.controller.message.MoveCause;
import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.controller.message.client.CMActivateHeroEffect;
import pp.winf2slay.controller.message.client.CMAttackMonster;
import pp.winf2slay.controller.message.client.CMDrawCard;
import pp.winf2slay.controller.message.client.CMEndTurn;
import pp.winf2slay.controller.message.client.CMMulligan;
import pp.winf2slay.controller.message.client.CMPlayCard;
import pp.winf2slay.controller.message.server.BCCardMoved;
import pp.winf2slay.controller.message.server.BCEffectActivated;
import pp.winf2slay.controller.message.server.BCMonsterAttacked;
import pp.winf2slay.controller.message.server.BCMulligan;
import pp.winf2slay.controller.message.server.BCRollResolved;
import pp.winf2slay.controller.message.server.BCTurnEnded;
import pp.winf2slay.controller.message.server.SMActionRejected;
import pp.winf2slay.controller.message.server.SMContinueTurn;
import pp.winf2slay.controller.message.server.SMTurnSwitch;
import pp.winf2slay.controller.server.ServerGameLogic;
import pp.winf2slay.controller.server.ServerState;
import pp.winf2slay.controller.server.TurnInfo;
import pp.winf2slay.controller.server.flow.ChallengeFlow;
import pp.winf2slay.controller.server.flow.DiceRollFlow;
import pp.winf2slay.controller.server.flow.EffectFlow;
import pp.winf2slay.model.Action;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.card.Spell;

import java.lang.System.Logger.Level;
import java.util.Optional;

/**
 * Laufende Partie: Der aktive Spieler führt Aktionen aus; jede Aktion wird
 * vollständig abgehandelt (Animationen, Herausforderungen, Würfe, Effekte),
 * bevor er mit {@link SMContinueTurn} die nächste Aktion wählen darf.
 *
 * <p>Alle Regeln (Aktionspunkte, Mindestanzahl Helden, einmal pro Zug je Held)
 * werden hier serverseitig geprüft.</p>
 */
public class TurnState extends ServerState {

    /**
     * @param logic Serverlogik
     */
    public TurnState(ServerGameLogic logic) {
        super(logic);
    }

    @Override
    public boolean isPlaying() {
        return true;
    }

    private Game game() {
        return logic.getGame();
    }

    private TurnInfo turn() {
        return logic.getTurn();
    }

    /**
     * Beginnt den Zug eines Spielers und informiert alle.
     *
     * @param index Index des Spielers in der Sitzreihenfolge
     */
    public void startTurn(int index) {
        logic.beginTurn(index);
        LOGGER.log(Level.INFO, "{0} ist am Zug", logic.getActivePlayer().getName());
        logic.sendToAll(new SMTurnSwitch(logic.getActivePlayer().getName()));
    }

    /**
     * Schließt eine Aktion ab: Hat der aktive Spieler gewonnen, endet das Spiel,
     * sonst darf er weiterspielen.
     */
    private void continueTurn() {
        Player active = logic.getActivePlayer();
        if (game().hasWon(active)) {
            String how = active.getMonsters().size() >= Game.MONSTERS_TO_WIN
                         ? " hat " + active.getMonsters().size() + " Monster besiegt."
                         : " hat " + active.getClassTypes().size() + " verschiedene Klassen versammelt.";
            logic.setState(new GameOverState(logic, active.getName(), active.getName() + how));
            return;
        }
        logic.send(active, new SMContinueTurn(turn().getActionPoints()));
    }

    /**
     * Prüft, ob die Nachricht vom aktiven Spieler stammt.
     *
     * @param from Absender
     * @return aktiver Spieler, falls er der Absender ist
     */
    private Optional<Player> activeSender(int from) {
        Player active = logic.getActivePlayer();
        if (active.getId() != from) {
            LOGGER.log(Level.WARNING, "Spieler {0} ist nicht am Zug", from);
            return Optional.empty();
        }
        return Optional.of(active);
    }

    private void reject(Player player, String reason) {
        LOGGER.log(Level.INFO, "Aktion von {0} abgelehnt: {1}", player.getName(), reason);
        logic.send(player, new SMActionRejected(reason, turn().getActionPoints()));
    }

    // ------------------------------------------------------------------
    // Aktionen
    // ------------------------------------------------------------------

    @Override
    public void received(CMDrawCard msg, int from) {
        activeSender(from).ifPresent(player -> {
            if (!turn().canAfford(Action.DRAW_CARD)) {
                reject(player, "Zu wenige Aktionspunkte.");
                return;
            }
            Optional<Card> card = game().drawSupportCard(player);
            if (card.isEmpty()) {
                reject(player, "Der Nachziehstapel ist leer.");
                return;
            }
            turn().spend(Action.DRAW_CARD);
            logic.broadcast(new BCCardMoved(player.getName(), player.getName(), card.get(), CardPosition.SUPPORT_DECK,
                                            CardPosition.PLAYER_HAND, MoveCause.DRAW), this::continueTurn);
        });
    }

    @Override
    public void received(CMPlayCard msg, int from) {
        activeSender(from).ifPresent(player -> {
            Card wanted = msg.getCard();
            if (!turn().canAfford(Action.PLAY_CARD)) {
                reject(player, "Zu wenige Aktionspunkte.");
            }
            else if (!(wanted instanceof Hero || wanted instanceof Spell) || !player.getHand().contains(wanted)) {
                reject(player, "Nur Helden und Zauber von der Hand können ausgespielt werden.");
            }
            else if (wanted instanceof Hero && !player.getGroup().hasFreeSlot()) {
                reject(player, "Deine Gruppe ist voll.");
            }
            else {
                turn().spend(Action.PLAY_CARD);
                Card card = game().moveHandToHover(player, wanted);
                logic.broadcast(new BCCardMoved(player.getName(), player.getName(), card, CardPosition.PLAYER_HAND,
                                                CardPosition.PLAYER_HOVER, MoveCause.PLAY),
                                () -> new ChallengeFlow(logic, player, card).run(survived -> afterChallenge(player, card, survived)));
            }
        });
    }

    private void afterChallenge(Player player, Card card, boolean survived) {
        if (!survived) {
            game().moveHoverToDiscard(player);
            logic.broadcast(new BCCardMoved(player.getName(), player.getName(), card, CardPosition.PLAYER_HOVER,
                                            CardPosition.DISCARD_PILE, MoveCause.PLAY_FAILED), this::continueTurn);
            return;
        }
        switch (card) {
            case Hero hero -> {
                game().moveHoverToGroup(player);
                turn().markUsed(hero);
                logic.broadcast(new BCCardMoved(player.getName(), player.getName(), hero, CardPosition.PLAYER_HOVER,
                                                CardPosition.PLAYER_GROUP, MoveCause.PLAY_SUCCEEDED), () -> {
                    if (game().hasWon(player))
                        continueTurn();
                    else
                        activateHero(player, hero);
                });
            }
            case Spell spell -> logic.broadcast(new BCEffectActivated(player.getName(), spell),
                                                () -> new EffectFlow(logic, player, spell.getEffect()).run(() -> {
                                                    game().moveHoverToDiscard(player);
                                                    logic.broadcast(new BCCardMoved(player.getName(), player.getName(), spell,
                                                                                    CardPosition.PLAYER_HOVER,
                                                                                    CardPosition.DISCARD_PILE,
                                                                                    MoveCause.PLAY_SUCCEEDED),
                                                                    this::continueTurn);
                                                }));
            default -> throw new IllegalStateException("Nicht spielbare Karte im Aktionsfeld: " + card);
        }
    }

    @Override
    public void received(CMActivateHeroEffect msg, int from) {
        activeSender(from).ifPresent(player -> {
            Optional<Hero> hero = player.getGroup().find(msg.getHero());
            if (!turn().canAfford(Action.ACTIVATE_HERO)) {
                reject(player, "Zu wenige Aktionspunkte.");
            }
            else if (hero.isEmpty()) {
                reject(player, "Dieser Held gehört nicht zu deiner Gruppe.");
            }
            else if (turn().isUsed(hero.get())) {
                reject(player, "Der Effekt dieses Helden wurde in diesem Zug schon genutzt.");
            }
            else {
                turn().spend(Action.ACTIVATE_HERO);
                turn().markUsed(hero.get());
                activateHero(player, hero.get());
            }
        });
    }

    /**
     * Würfelt für einen Heldeneffekt und löst ihn bei Erfolg aus.
     */
    private void activateHero(Player player, Hero hero) {
        logic.broadcast(new BCEffectActivated(player.getName(), hero), () ->
                new DiceRollFlow(logic, RollPurpose.HERO_EFFECT, player, hero.getThreshold(), true, player.getHeroBonus())
                        .run(result -> {
                            boolean success = result.getTotal() >= hero.getThreshold();
                            logic.broadcast(new BCRollResolved(player.getName(), RollPurpose.HERO_EFFECT, hero, success,
                                                               result.getTotal()), () -> {
                                if (success)
                                    new EffectFlow(logic, player, hero.getEffect()).run(this::continueTurn);
                                else
                                    continueTurn();
                            });
                        }));
    }

    @Override
    public void received(CMAttackMonster msg, int from) {
        activeSender(from).ifPresent(player -> {
            Optional<Monster> monster = game().getOpenMonsters().find(msg.getMonster());
            if (!turn().canAfford(Action.ATTACK_MONSTER)) {
                reject(player, "Ein Angriff kostet " + Action.ATTACK_MONSTER.getCost() + " Aktionspunkte.");
            }
            else if (player.getGroup().size() < Action.HEROES_TO_ATTACK) {
                reject(player, "Für einen Angriff brauchst du mindestens " + Action.HEROES_TO_ATTACK + " Helden.");
            }
            else if (monster.isEmpty()) {
                reject(player, "Dieses Monster liegt nicht mehr aus.");
            }
            else {
                turn().spend(Action.ATTACK_MONSTER);
                attack(player, monster.get());
            }
        });
    }

    private void attack(Player player, Monster monster) {
        logic.broadcast(new BCMonsterAttacked(player.getName(), monster), () ->
                new DiceRollFlow(logic, RollPurpose.MONSTER_ATTACK, player, monster.getThreshold(),
                                 monster.isHigherWins(), player.getAttackBonus())
                        .run(result -> {
                            boolean success = monster.isDefeatedBy(result.getTotal());
                            logic.broadcast(new BCRollResolved(player.getName(), RollPurpose.MONSTER_ATTACK, monster,
                                                               success, result.getTotal()), () -> {
                                if (success) {
                                    game().defeatMonster(player, monster);
                                    logic.broadcast(new BCCardMoved(player.getName(), player.getName(), monster,
                                                                    CardPosition.OPEN_MONSTERS,
                                                                    CardPosition.PLAYER_MONSTERS,
                                                                    MoveCause.MONSTER_DEFEATED), this::continueTurn);
                                }
                                else {
                                    new EffectFlow(logic, player, monster.getPenalty()).run(this::continueTurn);
                                }
                            });
                        }));
    }

    @Override
    public void received(CMMulligan msg, int from) {
        activeSender(from).ifPresent(player -> {
            if (!turn().canAfford(Action.MULLIGAN)) {
                reject(player, "Ein Mulligan kostet " + Action.MULLIGAN.getCost() + " Aktionspunkte.");
                return;
            }
            turn().spend(Action.MULLIGAN);
            Game.Mulligan result = game().mulligan(player);
            logic.broadcast(new BCMulligan(player.getName(), result.discarded(), result.drawn()), this::continueTurn);
        });
    }

    @Override
    public void received(CMEndTurn msg, int from) {
        activeSender(from).ifPresent(player -> {
            int next = logic.nextIndex();
            String nextName = logic.getPlayers().get(next).getName();
            logic.broadcast(new BCTurnEnded(player.getName(), nextName), () -> startTurn(next));
        });
    }

    // ------------------------------------------------------------------
    // Verbindungen
    // ------------------------------------------------------------------

    @Override
    public void playerDisconnected(int id) {
        logic.findPlayer(id).filter(p -> !p.isBot()).ifPresent(player -> {
            logic.replaceByBot(player);
            if (!logic.hasHumans()) {
                logic.setState(new GameOverState(logic, null, "Alle Spieler haben das Spiel verlassen."));
                return;
            }
            // Wartet das Spiel gerade auf eine Aktion dieses Spielers, übernimmt der Bot sofort.
            if (logic.getActivePlayer() == player && !logic.isBusy())
                logic.send(player, new SMContinueTurn(turn().getActionPoints()));
        });
    }
}
