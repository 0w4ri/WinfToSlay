package pp.winf2slay.controller.server.flow;

import pp.winf2slay.controller.message.CardPosition;
import pp.winf2slay.controller.message.MoveCause;
import pp.winf2slay.controller.message.client.CMHeroSelectionResponse;
import pp.winf2slay.controller.message.client.CMPlayerSelectionResponse;
import pp.winf2slay.controller.message.server.BCCardMoved;
import pp.winf2slay.controller.message.server.BCCardsMoved;
import pp.winf2slay.controller.message.server.SMHeroSelectionRequest;
import pp.winf2slay.controller.message.server.SMPlayerSelectionRequest;
import pp.winf2slay.controller.server.ServerGameLogic;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.effect.DestroyAllHeroes;
import pp.winf2slay.model.effect.DestroyHero;
import pp.winf2slay.model.effect.DestroyParty;
import pp.winf2slay.model.effect.DestroyRandomHeroes;
import pp.winf2slay.model.effect.DrawCards;
import pp.winf2slay.model.effect.DrawFromDiscard;
import pp.winf2slay.model.effect.Effect;
import pp.winf2slay.model.effect.EveryoneDiscards;
import pp.winf2slay.model.effect.PassiveEffect;
import pp.winf2slay.model.effect.SacrificeHero;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Löst einen Karteneffekt Schritt für Schritt auf.
 *
 * <p>Effekte mit Anzahl (z. B. „Ziehe 2 Karten“) werden entsprechend oft
 * wiederholt; jeder Schritt wird als eigener Broadcast animiert. Effekte mit
 * Zielwahl fragen den ausführenden Spieler vor jedem Schritt.</p>
 */
public class EffectFlow {

    private static final Logger LOGGER = System.getLogger(EffectFlow.class.getName());

    private final ServerGameLogic logic;
    private final Player caster;
    private final Effect effect;
    private Runnable onDone;
    private int remaining;

    /**
     * @param logic  Serverlogik
     * @param caster Spieler, dessen Karte den Effekt auslöst
     * @param effect Effekt
     */
    public EffectFlow(ServerGameLogic logic, Player caster, Effect effect) {
        this.logic = logic;
        this.caster = caster;
        this.effect = effect;
    }

    /**
     * Startet die Auflösung.
     *
     * @param onDone wird nach dem letzten Schritt aufgerufen
     */
    public void run(Runnable onDone) {
        this.onDone = onDone;
        this.remaining = effect.getCount();
        nextStep();
    }

    private Game game() {
        return logic.getGame();
    }

    private void nextStep() {
        if (remaining <= 0) {
            onDone.run();
            return;
        }
        remaining--;
        switch (effect) {
            case DrawCards draw -> drawCard(draw);
            case DrawFromDiscard drawDiscard -> drawFromDiscard(drawDiscard);
            case DestroyHero destroy -> destroyChosenHero(destroy);
            case DestroyRandomHeroes destroyRandom -> destroyRandomHero(destroyRandom);
            case DestroyParty destroyParty -> destroyParty(destroyParty);
            case DestroyAllHeroes destroyAll -> destroyAll(destroyAll);
            case EveryoneDiscards discard -> everyoneDiscards(discard);
            case SacrificeHero sacrifice -> sacrifice(sacrifice);
            case PassiveEffect passive -> {
                LOGGER.log(Level.WARNING, "Passiver Effekt {0} kann nicht aktiv ausgelöst werden", passive);
                finish();
            }
        }
    }

    /**
     * Bricht weitere Wiederholungen ab, z. B. wenn es keine Ziele mehr gibt.
     */
    private void finish() {
        remaining = 0;
        nextStep();
    }

    private void drawCard(DrawCards draw) {
        Optional<Card> card = draw.drawOne(game(), caster);
        if (card.isEmpty()) {
            finish();
            return;
        }
        logic.broadcast(move(caster, card.get(), CardPosition.SUPPORT_DECK, CardPosition.PLAYER_HAND, MoveCause.EFFECT_DRAW),
                        this::nextStep);
    }

    private void drawFromDiscard(DrawFromDiscard drawDiscard) {
        Optional<Card> card = drawDiscard.takeOne(game(), caster);
        if (card.isEmpty()) {
            finish();
            return;
        }
        logic.broadcast(move(caster, card.get(), CardPosition.DISCARD_PILE, CardPosition.PLAYER_HAND,
                             MoveCause.EFFECT_DRAW_DISCARD), this::nextStep);
    }

    private void destroyChosenHero(DestroyHero destroy) {
        List<Hero> targets = destroy.targetsFor(game(), caster);
        if (targets.isEmpty()) {
            finish();
            return;
        }
        logic.request(caster, new SMHeroSelectionRequest(targets), CMHeroSelectionResponse.class, response -> {
            Hero chosen = targets.stream().filter(h -> h.equals(response.getHero())).findFirst()
                                 .orElse(targets.getFirst());
            Player owner = destroy.destroy(game(), chosen).orElseThrow();
            logic.broadcast(move(owner, chosen, CardPosition.PLAYER_GROUP, CardPosition.DISCARD_PILE,
                                 MoveCause.EFFECT_DESTROY), this::nextStep);
        });
    }

    private void destroyRandomHero(DestroyRandomHeroes destroyRandom) {
        Optional<DestroyRandomHeroes.Destroyed> destroyed = destroyRandom.destroyOne(game());
        if (destroyed.isEmpty()) {
            finish();
            return;
        }
        logic.broadcast(move(destroyed.get().owner(), destroyed.get().hero(), CardPosition.PLAYER_GROUP,
                             CardPosition.DISCARD_PILE, MoveCause.EFFECT_DESTROY_RANDOM), this::nextStep);
    }

    private void destroyParty(DestroyParty destroyParty) {
        List<Player> targets = destroyParty.targets(game());
        if (targets.isEmpty()) {
            finish();
            return;
        }
        List<String> names = targets.stream().map(Player::getName).toList();
        logic.request(caster, new SMPlayerSelectionRequest(names), CMPlayerSelectionResponse.class, response -> {
            Player target = targets.stream().filter(p -> p.getName().equals(response.getPlayerName())).findFirst()
                                   .orElse(targets.getFirst());
            List<Hero> heroes = destroyParty.destroyParty(game(), target);
            logic.broadcast(BCCardsMoved.of(Map.of(target.getName(), heroes), CardPosition.PLAYER_GROUP,
                                            CardPosition.DISCARD_PILE, MoveCause.EFFECT_DESTROY_PARTY), this::nextStep);
        });
    }

    private void destroyAll(DestroyAllHeroes destroyAll) {
        Map<Player, List<Hero>> destroyed = destroyAll.destroyAll(game());
        if (destroyed.isEmpty()) {
            finish();
            return;
        }
        Map<String, List<Hero>> byName = new LinkedHashMap<>();
        destroyed.forEach((p, heroes) -> byName.put(p.getName(), heroes));
        logic.broadcast(BCCardsMoved.of(byName, CardPosition.PLAYER_GROUP, CardPosition.DISCARD_PILE,
                                        MoveCause.EFFECT_DESTROY_ALL), this::nextStep);
    }

    private void everyoneDiscards(EveryoneDiscards discard) {
        Map<Player, Card> discarded = discard.discardOneEach(game());
        if (discarded.isEmpty()) {
            finish();
            return;
        }
        Map<String, List<Card>> byName = new LinkedHashMap<>();
        discarded.forEach((p, card) -> byName.put(p.getName(), List.of(card)));
        logic.broadcast(BCCardsMoved.of(byName, CardPosition.PLAYER_HAND, CardPosition.DISCARD_PILE,
                                        MoveCause.EFFECT_DISCARD), this::nextStep);
    }

    private void sacrifice(SacrificeHero sacrifice) {
        Optional<Hero> victim = sacrifice.sacrifice(game(), caster);
        if (victim.isEmpty()) {
            finish();
            return;
        }
        logic.broadcast(move(caster, victim.get(), CardPosition.PLAYER_GROUP, CardPosition.DISCARD_PILE,
                             MoveCause.MONSTER_PENALTY), this::nextStep);
    }

    private BCCardMoved move(Player owner, Card card, CardPosition from, CardPosition to, MoveCause cause) {
        return new BCCardMoved(caster.getName(), owner.getName(), card, from, to, cause);
    }
}
