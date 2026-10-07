package pp.winf2slay.model.deck;

import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Challenge;
import pp.winf2slay.model.card.ClassType;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Leader;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.card.Spell;
import pp.winf2slay.model.effect.AttackBonus;
import pp.winf2slay.model.effect.ChallengeBonus;
import pp.winf2slay.model.effect.DestroyAllHeroes;
import pp.winf2slay.model.effect.DestroyHero;
import pp.winf2slay.model.effect.DestroyParty;
import pp.winf2slay.model.effect.DestroyRandomHeroes;
import pp.winf2slay.model.effect.DrawCards;
import pp.winf2slay.model.effect.DrawFromDiscard;
import pp.winf2slay.model.effect.Effect;
import pp.winf2slay.model.effect.EveryoneDiscards;
import pp.winf2slay.model.effect.HeroBonus;
import pp.winf2slay.model.effect.SacrificeHero;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Vollständiger Kartenkatalog von WinfToSlay.
 *
 * <p>Alle Kartendaten (Werte, Effekte, Anzeigenamen) stehen an genau dieser
 * Stelle. Die technischen Namen entsprechen den 3D-Modellen unter
 * {@code view/src/main/resources/cards}.</p>
 */
public final class CardCatalog {

    /**
     * Personen auf den Heldenkarten – für jede Klasse gibt es dieselben acht Helden.
     */
    private static final String[] HERO_NAMES = {
            "Falck", "Pyritz", "Kerling", "Mersteiner", "Küster", "Rogowski", "Becker", "Schröter"
    };

    /**
     * Mindestwurf je Heldennummer (1–8).
     */
    private static final int[] HERO_THRESHOLDS = {4, 10, 6, 7, 8, 6, 7, 3};

    private static final String[] CLASS_PREFIX = {"Mage", "Guard", "Fighter", "Ranger", "Thief", "Bard"};

    private static final int CHALLENGE_COUNT = 20;

    private CardCatalog() {
        // Utility-Klasse
    }

    /**
     * Erzeugt den gemischten Unterstützungsstapel (Helden, Zauber, Modifikationen,
     * Herausforderungen).
     *
     * @param rng Zufallsgenerator zum Mischen
     * @return Stapel
     */
    public static Deck<Card> createSupportDeck(Random rng) {
        return new Deck<>(supportCards(), rng);
    }

    /**
     * Erzeugt den gemischten Monsterstapel.
     *
     * @param rng Zufallsgenerator zum Mischen
     * @return Stapel
     */
    public static Deck<Monster> createMonsterDeck(Random rng) {
        return new Deck<>(monsters(), rng);
    }

    /**
     * Erzeugt den gemischten Anführerstapel.
     *
     * @param rng Zufallsgenerator zum Mischen
     * @return Stapel
     */
    public static Deck<Leader> createLeaderDeck(Random rng) {
        return new Deck<>(leaders(), rng);
    }

    /**
     * @return alle Karten des Unterstützungsstapels (ungemischt)
     */
    public static List<Card> supportCards() {
        List<Card> cards = new ArrayList<>(heroes());
        cards.addAll(spells());
        cards.addAll(modifications());
        for (int i = 0; i < CHALLENGE_COUNT; i++) {
            cards.add(new Challenge());
        }
        return cards;
    }

    /**
     * @return alle 48 Helden (sechs Klassen à acht Helden)
     */
    public static List<Hero> heroes() {
        List<Hero> heroes = new ArrayList<>();
        ClassType[] types = {ClassType.MAGE, ClassType.GUARD, ClassType.FIGHTER,
                             ClassType.RANGER, ClassType.THIEF, ClassType.BARD};
        for (int c = 0; c < types.length; c++) {
            for (int i = 0; i < HERO_NAMES.length; i++) {
                String name = CLASS_PREFIX[c] + "_" + (i + 1);
                heroes.add(new Hero(name, HERO_NAMES[i], HERO_THRESHOLDS[i], heroEffect(i + 1), types[c]));
            }
        }
        return heroes;
    }

    /**
     * Effekt eines Helden abhängig von seiner Nummer – alle Klassen sind gleich aufgebaut.
     *
     * @param number Heldennummer 1–8
     * @return Effekt
     */
    private static Effect heroEffect(int number) {
        return switch (number) {
            case 1 -> new DrawCards(2);
            case 2 -> new EveryoneDiscards(2);
            case 3 -> new DestroyHero(1);
            case 4 -> new EveryoneDiscards(1);
            case 5 -> new DrawFromDiscard(2);
            case 6 -> new DrawFromDiscard(1);
            case 7 -> new DestroyHero(2);
            case 8 -> new DrawCards(1);
            default -> throw new IllegalArgumentException("Heldennummer " + number);
        };
    }

    /**
     * @return alle Zauber
     */
    public static List<Spell> spells() {
        List<Spell> spells = new ArrayList<>();
        for (int i = 0; i < 2; i++)
            spells.add(new Spell("Spell_1", "50 Freedoms / Second", new DestroyParty()));
        spells.add(new Spell("Spell_2", "GBU48", new DestroyAllHeroes()));
        for (int i = 0; i < 2; i++)
            spells.add(new Spell("Spell_3", "Infanterie-Frühstück", new DrawCards(2)));
        for (int i = 0; i < 2; i++)
            spells.add(new Spell("Spell_4", "Mörser 120mm", new DestroyRandomHeroes(2)));
        return spells;
    }

    /**
     * @return alle Modifikationen
     */
    public static List<Modification> modifications() {
        List<Modification> mods = new ArrayList<>();
        addCopies(mods, 3, 4);
        addCopies(mods, 3, -4);
        addCopies(mods, 5, 2);
        addCopies(mods, 5, -2);
        return mods;
    }

    private static void addCopies(List<Modification> mods, int copies, int delta) {
        for (int i = 0; i < copies; i++) {
            mods.add(new Modification(delta));
        }
    }

    /**
     * @return alle Monster
     */
    public static List<Monster> monsters() {
        return List.of(
                monster("DjinnMerz", "Djinn-März", 8, true, new HeroBonus(1)),
                monster("Dusche", "Dusche", 8, true, new HeroBonus(1)),
                monster("DieMumie", "Die Mumie", 8, true, new AttackBonus(1)),
                monster("Clownimel", "Clownimel", 8, true, new ChallengeBonus(1)),
                monster("Annarachne", "Annarachne", 8, true, new AttackBonus(1)),
                monster("VampArno", "VampArno", 10, true, new AttackBonus(1)),
                monster("Tentakulus", "Tentakulus", 9, true, new ChallengeBonus(1)),
                monster("SSSSiegle", "SSSSiegle", 8, true, new AttackBonus(1)),
                monster("OberstGhoul", "Oberst Ghul", 8, true, new AttackBonus(1)),
                monster("Minastaurus", "Minastaurus", 9, true, new ChallengeBonus(1)),
                monster("KochEnt", "KochEnt", 10, true, new ChallengeBonus(1)),
                monster("Klausuren", "Klausuren", 8, true, new HeroBonus(1)),
                monster("Johanna", "Johanna", 8, true, new ChallengeBonus(1)),
                monster("Hommelgoyle", "Hommelgoyle", 7, false, new HeroBonus(1)),
                monster("Hexidentin", "Hexidentin", 11, true, new HeroBonus(1))
        );
    }

    private static Monster monster(String name, String displayName, int threshold, boolean higherWins, Effect reward) {
        return new Monster(name, displayName, threshold, higherWins, new SacrificeHero(), reward);
    }

    /**
     * @return alle Anführer
     */
    public static List<Leader> leaders() {
        return List.of(
                new Leader("Luciano", "Luciano", new ChallengeBonus(1), ClassType.MAGE),
                new Leader("Armin", "Armin Fortenbacher", new ChallengeBonus(1), ClassType.FIGHTER),
                new Leader("187", "187 Strassenbande", new AttackBonus(1), ClassType.THIEF),
                new Leader("Merkel", "Angela Merkel", new HeroBonus(1), ClassType.GUARD),
                new Leader("SSIO", "SSIO", new HeroBonus(1), ClassType.BARD),
                new Leader("LEGOlas", "LEGOlas", new AttackBonus(1), ClassType.RANGER)
        );
    }
}
