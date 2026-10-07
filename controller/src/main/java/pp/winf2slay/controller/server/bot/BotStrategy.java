package pp.winf2slay.controller.server.bot;

import pp.winf2slay.controller.message.BotLevel;
import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.controller.message.client.CMActivateHeroEffect;
import pp.winf2slay.controller.message.client.CMAttackMonster;
import pp.winf2slay.controller.message.client.CMDrawCard;
import pp.winf2slay.controller.message.client.CMEndTurn;
import pp.winf2slay.controller.message.client.CMMulligan;
import pp.winf2slay.controller.message.client.CMPlayCard;
import pp.winf2slay.controller.message.client.ClientMessage;
import pp.winf2slay.model.Action;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Challenge;
import pp.winf2slay.model.card.ClassType;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.card.Spell;
import pp.winf2slay.model.die.DiceResult;
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
import pp.winf2slay.model.field.Group;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Entscheidungslogik der Bots.
 *
 * <p>Der Bot bewertet alle erlaubten Aktionen mit einer einfachen Heuristik
 * (Gewinnwahrscheinlichkeit der Würfe, Nähe zur Siegbedingung, Bedrohung durch
 * Gegner) und wählt die beste. Die {@link BotLevel Spielstärke} bestimmt, wie
 * oft der Bot vom besten Zug abweicht und wie konsequent er Herausforderungen
 * und Modifikationen einsetzt.</p>
 */
public class BotStrategy {

    /**
     * Wahl bei einer Modifikation während einer Herausforderung.
     *
     * @param modification gespielte Karte
     * @param targetPlayer Spieler, dessen Wurf verändert wird
     */
    public record DoubleChoice(Modification modification, String targetPlayer) {}

    /**
     * Bewertete Aktion.
     */
    private record Candidate(ClientMessage message, double score) {}

    /** Mindestbewertung, damit der Bot eine Aktion ausführt statt den Zug zu beenden. */
    private static final double MIN_SCORE = 0.25;

    private final BotLevel level;
    private final Random random;
    private final double mistakeRate;
    private final double challengeBase;
    private final double modifyEagerness;
    private final double attackThreshold;

    /**
     * @param level  Spielstärke
     * @param random Zufallsgenerator (für reproduzierbare Tests übergebbar)
     */
    public BotStrategy(BotLevel level, Random random) {
        this.level = level;
        this.random = random;
        switch (level) {
            case EASY -> {
                mistakeRate = 0.45;
                challengeBase = 0.02;
                modifyEagerness = 0.3;
                attackThreshold = 0.45;
            }
            case HARD -> {
                mistakeRate = 0.0;
                challengeBase = 0.06;
                modifyEagerness = 1.0;
                attackThreshold = 0.33;
            }
            default -> {
                mistakeRate = 0.1;
                challengeBase = 0.04;
                modifyEagerness = 0.8;
                attackThreshold = 0.35;
            }
        }
    }

    /**
     * @param level Spielstärke
     * @return Strategie mit eigenem Zufallsgenerator
     */
    public static BotStrategy forLevel(BotLevel level) {
        return new BotStrategy(level, new Random());
    }

    /**
     * @return Spielstärke
     */
    public BotLevel getLevel() {
        return level;
    }

    // ------------------------------------------------------------------
    // Eigener Zug
    // ------------------------------------------------------------------

    /**
     * Wählt die nächste Aktion im eigenen Zug.
     *
     * @param ctx Spielsicht
     * @return Aktion (im Zweifel {@link CMEndTurn})
     */
    public ClientMessage chooseAction(BotContext ctx) {
        if (!ctx.myTurn()) return new CMEndTurn();
        List<Candidate> candidates = new ArrayList<>();
        addAttacks(ctx, candidates);
        addPlays(ctx, candidates);
        addActivations(ctx, candidates);
        addDraw(ctx, candidates);
        addMulligan(ctx, candidates);
        candidates.removeIf(c -> c.score() < MIN_SCORE);
        if (candidates.isEmpty()) return new CMEndTurn();
        if (random.nextDouble() < mistakeRate)
            return candidates.get(random.nextInt(candidates.size())).message();
        return candidates.stream().max(Comparator.comparingDouble(Candidate::score)).orElseThrow().message();
    }

    private void addAttacks(BotContext ctx, List<Candidate> candidates) {
        Player me = ctx.me();
        if (!ctx.canAfford(Action.ATTACK_MONSTER) || me.getGroup().size() < Action.HEROES_TO_ATTACK) return;
        int defeated = me.getMonsters().size();
        // Volle Gruppe ohne Aussicht auf sechs Klassen: Ein verlorener Kampf macht sogar Platz.
        boolean stuck = !me.getGroup().hasFreeSlot() && ctx.myClasses().size() < Game.CLASSES_TO_WIN;
        for (Monster monster : ctx.openMonsters()) {
            double p = Odds.success(monster.getThreshold(), monster.isHigherWins(), me.getAttackBonus());
            boolean winningShot = defeated == Game.MONSTERS_TO_WIN - 1;
            double minChance = stuck ? 0.1 : attackThreshold;
            if (p < minChance && !(winningShot && p >= 0.25)) continue;
            double value = winningShot ? 12 : 5 + 2.5 * defeated + rewardValue(monster);
            double loss = stuck ? 0 : 1.0; // verlorener Kampf kostet einen Helden
            candidates.add(new Candidate(new CMAttackMonster(monster), p * value - (1 - p) * loss));
        }
    }

    private double rewardValue(Monster monster) {
        return monster.getReward() instanceof PassiveEffect ? 0.5 : 0;
    }

    private void addPlays(BotContext ctx, List<Candidate> candidates) {
        if (!ctx.canAfford(Action.PLAY_CARD)) return;
        Player me = ctx.me();
        Set<ClassType> classes = ctx.myClasses();
        for (Card card : distinct(ctx.hand())) {
            if (card instanceof Hero hero && me.getGroup().hasFreeSlot()) {
                boolean newClass = !classes.contains(hero.getClassType());
                boolean winsNow = newClass && classes.size() + 1 >= Game.CLASSES_TO_WIN;
                int missingClasses = Game.CLASSES_TO_WIN - classes.size();
                int freeSlots = Group.SIZE - me.getGroup().size();
                double effect = effectValue(ctx, hero.getEffect())
                                * Odds.success(hero.getThreshold(), true, me.getHeroBonus());
                double base;
                if (winsNow) base = 100;
                else if (newClass) base = 4.7;
                else if (freeSlots > missingClasses) base = 2.0;  // Platz für doppelte Klassen
                else base = 0.4;                                     // würde den Klassensieg blockieren
                double score = base + 0.8 * effect;
                candidates.add(new Candidate(new CMPlayCard(hero), score));
            }
            else if (card instanceof Spell spell) {
                candidates.add(new Candidate(new CMPlayCard(spell), effectValue(ctx, spell.getEffect())));
            }
        }
    }

    private void addActivations(BotContext ctx, List<Candidate> candidates) {
        if (!ctx.canAfford(Action.ACTIVATE_HERO)) return;
        Player me = ctx.me();
        for (Hero hero : me.getGroup().getHeroes()) {
            if (ctx.turn().isUsed(hero)) continue;
            double p = Odds.success(hero.getThreshold(), true, me.getHeroBonus());
            double score = p * effectValue(ctx, hero.getEffect()) - 0.2;
            candidates.add(new Candidate(new CMActivateHeroEffect(hero), score));
        }
    }

    private void addDraw(BotContext ctx, List<Candidate> candidates) {
        if (!ctx.canAfford(Action.DRAW_CARD)) return;
        Game game = ctx.game();
        if (game.getSupportDeck().isEmpty() && game.getDiscardPile().isEmpty()) return;
        int handSize = ctx.hand().size();
        // Viele Handkarten bringen wenig – der Bot soll nicht den ganzen Stapel horten.
        double score = 1.0 + (handSize < 4 ? 0.6 : 0) - (handSize > 7 ? 0.8 : 0) - (handSize > 11 ? 1.0 : 0);
        candidates.add(new Candidate(new CMDrawCard(), score));
    }

    private void addMulligan(BotContext ctx, List<Candidate> candidates) {
        if (!ctx.canAfford(Action.MULLIGAN) || ctx.actionPoints() != Action.MULLIGAN.getCost()) return;
        boolean playable = ctx.hand().stream().anyMatch(c -> c instanceof Hero || c instanceof Spell);
        if (!playable && ctx.hand().size() >= 3)
            candidates.add(new Candidate(new CMMulligan(), 1.8));
    }

    /**
     * Schätzt den Nutzen eines Effekts für den Bot ab (grob in „Karten“ gemessen).
     *
     * @param ctx    Spielsicht
     * @param effect Effekt
     * @return Nutzen (negativ, wenn der Effekt dem Bot eher schadet)
     */
    double effectValue(BotContext ctx, Effect effect) {
        Player me = ctx.me();
        Game game = ctx.game();
        int myHeroes = me.getGroup().size();
        int oppHeroes = game.getHeroesOfOpponents(me).size();
        Player strongest = ctx.strongestOpponent();
        double threat = strongest == null ? 0 : ctx.progress(strongest);
        return switch (effect) {
            case DrawCards draw -> 0.9 * draw.getCount();
            case DrawFromDiscard draw -> game.getDiscardPile().isEmpty() ? 0
                                         : 0.8 * Math.min(draw.getCount(), game.getDiscardPile().size());
            case DestroyHero destroy -> oppHeroes == 0 ? 0
                                        : 1.6 * Math.min(destroy.getCount(), oppHeroes) + 1.5 * threat;
            case DestroyRandomHeroes destroy -> {
                int total = myHeroes + oppHeroes;
                if (total == 0) yield 0;
                double share = oppHeroes / (double) total;
                yield destroy.getCount() * (1.5 * share - 1.8 * (1 - share));
            }
            case DestroyParty ignored -> {
                int best = ctx.opponents().stream().mapToInt(p -> p.getGroup().size()).max().orElse(0);
                yield best == 0 ? (myHeroes > 0 ? -2 : 0) : 1.2 * best + 1.5 * threat;
            }
            case DestroyAllHeroes ignored -> 1.3 * oppHeroes - 1.6 * myHeroes;
            case EveryoneDiscards discard -> {
                long victims = ctx.opponents().stream().filter(p -> !p.getHand().isEmpty()).count();
                yield discard.getCount() * (0.4 * victims - (ctx.hand().size() > 1 ? 0.6 : 0.1));
            }
            case SacrificeHero ignored -> -1.5;
            case PassiveEffect ignored -> 0.5;
        };
    }

    // ------------------------------------------------------------------
    // Reaktionen auf andere Spieler
    // ------------------------------------------------------------------

    /**
     * Entscheidet, ob eine ausgespielte Karte herausgefordert wird.
     *
     * @param ctx          Spielsicht
     * @param activePlayer Spieler, der die Karte ausgespielt hat
     * @param card         ausgespielte Karte
     * @return gespielte Herausforderung oder leer
     */
    public Optional<Challenge> decideChallenge(BotContext ctx, String activePlayer, Card card) {
        Optional<Challenge> challenge = ctx.hand().stream().filter(Challenge.class::isInstance)
                                           .map(Challenge.class::cast).findFirst();
        Player active = ctx.game().findPlayer(activePlayer).orElse(null);
        if (challenge.isEmpty() || active == null) return Optional.empty();

        double urgency;
        if (card instanceof Hero hero) {
            Set<ClassType> classes = active.getClassTypes();
            boolean newClass = !classes.contains(hero.getClassType());
            if (newClass && classes.size() + 1 >= Game.CLASSES_TO_WIN)
                urgency = level == BotLevel.EASY ? 0.4 : 0.95;      // würde sofort gewinnen
            else if (newClass && classes.size() + 2 >= Game.CLASSES_TO_WIN)
                urgency = 0.35;                                      // kommt dem Sieg sehr nahe
            else
                urgency = challengeBase;
        }
        else if (card instanceof Spell spell) {
            urgency = Math.max(challengeBase, spellThreat(ctx, spell.getEffect()));
        }
        else {
            urgency = challengeBase;
        }
        // Gegen sehr starke Bonuswerte lohnt sich eine Herausforderung weniger.
        double winChance = 1 - Odds.beats(active.getChallengeBonus(), ctx.me().getChallengeBonus());
        double probability = urgency * (0.5 + winChance);
        return random.nextDouble() < probability ? challenge : Optional.empty();
    }

    private double spellThreat(BotContext ctx, Effect effect) {
        int myHeroes = ctx.me().getGroup().size();
        int maxOther = ctx.opponents().stream().mapToInt(p -> p.getGroup().size()).max().orElse(0);
        return switch (effect) {
            case DestroyAllHeroes ignored -> myHeroes >= 2 ? 0.6 : 0.1;
            case DestroyParty ignored -> myHeroes >= 2 && myHeroes >= maxOther ? 0.45 : 0.05;
            case DestroyRandomHeroes ignored -> myHeroes >= 3 ? 0.15 : 0.03;
            default -> 0.02;
        };
    }

    /**
     * Entscheidet, ob ein einzelner Wurf modifiziert wird.
     *
     * @param ctx        Spielsicht
     * @param result     aktuelles Ergebnis
     * @param purpose    Anlass des Wurfs
     * @param threshold  Schwellenwert
     * @param higherWins Richtung des Schwellenwerts
     * @return gespielte Modifikation oder leer
     */
    public Optional<Modification> decideModification(BotContext ctx, DiceResult result, RollPurpose purpose,
                                                     int threshold, boolean higherWins) {
        List<Modification> mods = myModifications(ctx);
        if (mods.isEmpty() || random.nextDouble() > modifyEagerness) return Optional.empty();
        boolean success = higherWins ? result.getTotal() >= threshold : result.getTotal() <= threshold;
        boolean mine = ctx.me().getName().equals(result.getPlayerName());
        if (mine && !success) {
            return smallest(mods, d -> higherWins ? result.getTotal() + d >= threshold : result.getTotal() + d <= threshold);
        }
        if (!mine && success) {
            // Gegnerische Erfolge werden vor allem dann vereitelt, wenn der Gegner dem Sieg nahe ist.
            Player roller = ctx.game().findPlayer(result.getPlayerName()).orElse(null);
            double importance = purpose == RollPurpose.MONSTER_ATTACK ? 0.45 : 0.2;
            if (roller != null) importance += ctx.progress(roller) * 0.6;
            if (level == BotLevel.HARD) importance *= 1.2;
            if (random.nextDouble() > importance) return Optional.empty();
            return smallest(mods, d -> higherWins ? result.getTotal() + d < threshold : result.getTotal() + d > threshold);
        }
        return Optional.empty();
    }

    /**
     * Entscheidet, ob einer der beiden Würfe einer Herausforderung modifiziert wird.
     *
     * @param ctx              Spielsicht
     * @param activeResult     Wurf des aktiven Spielers
     * @param challengerResult Wurf des Herausforderers
     * @return Wahl oder leer
     */
    public Optional<DoubleChoice> decideChallengeModification(BotContext ctx, DiceResult activeResult,
                                                              DiceResult challengerResult) {
        List<Modification> mods = myModifications(ctx);
        if (mods.isEmpty() || random.nextDouble() > modifyEagerness) return Optional.empty();
        String me = ctx.me().getName();
        String active = activeResult.getPlayerName();
        String challenger = challengerResult.getPlayerName();
        int a = activeResult.getTotal();
        int c = challengerResult.getTotal();
        boolean activeWins = a > c;

        boolean helpActive = me.equals(active);
        boolean helpChallenger = me.equals(challenger);
        if (!helpActive && !helpChallenger && level == BotLevel.HARD) {
            // Unbeteiligte Bots helfen gegen den führenden Spieler.
            Player activePlayer = ctx.game().findPlayer(active).orElse(null);
            helpChallenger = activePlayer != null && activePlayer == ctx.strongestOpponent();
        }
        if (helpActive && !activeWins) {
            Optional<Modification> raise = smallest(mods, d -> d > 0 && a + d > c);
            if (raise.isPresent()) return Optional.of(new DoubleChoice(raise.get(), active));
            return smallest(mods, d -> d < 0 && a > c + d).map(m -> new DoubleChoice(m, challenger));
        }
        if (helpChallenger && activeWins) {
            Optional<Modification> lower = smallest(mods, d -> d < 0 && a + d <= c);
            if (lower.isPresent()) return Optional.of(new DoubleChoice(lower.get(), active));
            return smallest(mods, d -> d > 0 && a <= c + d).map(m -> new DoubleChoice(m, challenger));
        }
        return Optional.empty();
    }

    /**
     * Wählt einen gegnerischen Helden zum Zerstören.
     *
     * @param ctx        Spielsicht
     * @param candidates wählbare Helden
     * @return gewählter Held
     */
    public Hero chooseHeroToDestroy(BotContext ctx, List<Hero> candidates) {
        if (level == BotLevel.EASY) return candidates.get(random.nextInt(candidates.size()));
        return candidates.stream().max(Comparator.comparingDouble(h -> heroThreat(ctx, h))).orElseThrow();
    }

    private double heroThreat(BotContext ctx, Hero hero) {
        Player owner = ctx.ownerOf(hero);
        if (owner == null) return 0;
        double score = ctx.progress(owner) * 3 + owner.getMonsters().size() * 0.5;
        long sameClass = owner.getGroup().getHeroes().stream().filter(h -> h.getClassType() == hero.getClassType()).count();
        boolean leaderClass = owner.getLeader() != null && owner.getLeader().getClassType() == hero.getClassType();
        if (sameClass == 1 && !leaderClass) score += 2; // zerstört eine Klasse für die Siegbedingung
        return score;
    }

    /**
     * Wählt den Spieler, dessen Gruppe zerstört wird.
     *
     * @param ctx        Spielsicht
     * @param candidates wählbare Spieler (kann den Bot selbst enthalten)
     * @return gewählter Spielername
     */
    public String choosePlayerToRaid(BotContext ctx, List<String> candidates) {
        List<String> others = candidates.stream().filter(n -> !n.equals(ctx.me().getName())).toList();
        if (others.isEmpty()) return candidates.getFirst();
        if (level == BotLevel.EASY) return others.get(random.nextInt(others.size()));
        return others.stream().max(Comparator.comparingDouble(name -> {
            Player p = ctx.game().findPlayer(name).orElse(null);
            return p == null ? 0 : p.getGroup().size() + 3 * ctx.progress(p);
        })).orElseThrow();
    }

    // ------------------------------------------------------------------
    // Hilfsfunktionen
    // ------------------------------------------------------------------

    private static List<Modification> myModifications(BotContext ctx) {
        return ctx.hand().stream().filter(Modification.class::isInstance).map(Modification.class::cast).toList();
    }

    /**
     * Wählt die Modifikation mit dem kleinsten Betrag, die die Bedingung erfüllt.
     */
    private static Optional<Modification> smallest(List<Modification> mods, IntPredicate condition) {
        return mods.stream().filter(m -> condition.test(m.getDelta()))
                   .min(Comparator.comparingInt(m -> Math.abs(m.getDelta())));
    }

    private static List<Card> distinct(List<Card> cards) {
        return cards.stream().distinct().toList();
    }
}
