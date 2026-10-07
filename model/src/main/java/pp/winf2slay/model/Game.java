package pp.winf2slay.model;

import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Leader;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.deck.CardCatalog;
import pp.winf2slay.model.deck.Deck;
import pp.winf2slay.model.die.DiceResult;
import pp.winf2slay.model.die.PairOfDice;
import pp.winf2slay.model.effect.PassiveEffect;
import pp.winf2slay.model.field.MonsterField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Vollständiger Spielzustand: Stapel, Ablagestapel, offene Monster, Spieler,
 * Würfel und Zufallsgenerator.
 *
 * <p>Alle Kartenbewegungen laufen über die {@code move…}-Methoden dieser Klasse,
 * damit Karten nie verloren gehen oder doppelt existieren.</p>
 */
public class Game {

    /**
     * Maximale Spielerzahl (begrenzt durch die Anzahl der Anführer).
     */
    public static final int MAX_PLAYERS = 6;

    /**
     * Anzahl verschiedener Klassen (inklusive Anführer) für einen Sieg.
     *
     * <p>Die ursprüngliche Implementierung prüfte auf fünf Klassen; dieser Wert
     * wurde beibehalten, weil Partien mit sechs Klassen (Anführer + fünf
     * Helden, also eine komplett gefüllte Gruppe ohne Doppelungen) in
     * Simulationen etwa doppelt so lange dauern. Für die „harte“ Regel einfach
     * auf {@code ClassType.values().length} setzen.</p>
     */
    public static final int CLASSES_TO_WIN = 5;

    /**
     * Anzahl besiegter Monster für einen Sieg.
     */
    public static final int MONSTERS_TO_WIN = MonsterField.SIZE;

    /**
     * Anzahl der Startkarten (auch nach einem Mulligan).
     */
    public static final int HAND_SIZE = 5;

    /**
     * Fällt der Unterstützungsstapel unter diese Größe, wird der Ablagestapel
     * daruntergemischt.
     */
    private static final int REFILL_THRESHOLD = 5;

    private final Random random;
    private final Deck<Card> supportDeck;
    private final Deck<Monster> monsterDeck;
    private final Deck<Leader> leaderDeck;
    private final List<Card> discardPile = new ArrayList<>();
    private final MonsterField openMonsters = new MonsterField();
    private final List<Player> players = new ArrayList<>();
    private final PairOfDice dice;

    /**
     * Erzeugt ein Spiel mit zufälliger Kartenreihenfolge.
     */
    public Game() {
        this(new Random());
    }

    /**
     * Erzeugt ein reproduzierbares Spiel (für Tests).
     *
     * @param seed Startwert des Zufallsgenerators
     */
    public Game(long seed) {
        this(new Random(seed));
    }

    /**
     * Erzeugt ein Spiel mit dem angegebenen Zufallsgenerator.
     *
     * @param random Zufallsgenerator für Mischen und Würfeln
     */
    public Game(Random random) {
        this.random = Objects.requireNonNull(random, "random");
        supportDeck = CardCatalog.createSupportDeck(random);
        monsterDeck = CardCatalog.createMonsterDeck(random);
        leaderDeck = CardCatalog.createLeaderDeck(random);
        dice = new PairOfDice(random);
        refillMonsters();
    }

    // ------------------------------------------------------------------
    // Zugriff
    // ------------------------------------------------------------------

    /**
     * @return Unterstützungsstapel
     */
    public Deck<Card> getSupportDeck() {
        return supportDeck;
    }

    /**
     * @return Monsterstapel
     */
    public Deck<Monster> getMonsterDeck() {
        return monsterDeck;
    }

    /**
     * @return Anführerstapel
     */
    public Deck<Leader> getLeaderDeck() {
        return leaderDeck;
    }

    /**
     * @return unveränderliche Sicht auf den Ablagestapel (oberste Karte zuletzt)
     */
    public List<Card> getDiscardPile() {
        return Collections.unmodifiableList(discardPile);
    }

    /**
     * @return offen ausliegende Monster
     */
    public MonsterField getOpenMonsters() {
        return openMonsters;
    }

    /**
     * @return Spieler in Zugreihenfolge
     */
    public List<Player> getPlayers() {
        return players;
    }

    /**
     * @return Zufallsgenerator des Spiels
     */
    public Random getRandom() {
        return random;
    }

    // ------------------------------------------------------------------
    // Spielerverwaltung
    // ------------------------------------------------------------------

    /**
     * Nimmt einen Spieler auf.
     *
     * @param player neuer Spieler
     * @throws IllegalStateException wenn bereits {@value #MAX_PLAYERS} Spieler teilnehmen
     */
    public void addPlayer(Player player) {
        if (players.size() >= MAX_PLAYERS)
            throw new IllegalStateException("Maximale Spieleranzahl erreicht");
        players.add(Objects.requireNonNull(player, "player"));
    }

    /**
     * Entfernt einen Spieler (nur vor Spielbeginn sinnvoll).
     *
     * @param player Spieler
     * @return {@code true}, wenn der Spieler teilgenommen hat
     */
    public boolean removePlayer(Player player) {
        return players.remove(player);
    }

    /**
     * @param id Spieler-ID
     * @return Spieler mit dieser ID
     */
    public Optional<Player> findPlayer(int id) {
        return players.stream().filter(p -> p.getId() == id).findFirst();
    }

    /**
     * @param name Spielername
     * @return Spieler mit diesem Namen
     */
    public Optional<Player> findPlayer(String name) {
        return players.stream().filter(p -> p.getName().equals(name)).findFirst();
    }

    /**
     * Zieht einen Anführer.
     *
     * @return Anführer; leer, wenn alle vergeben sind
     */
    public Optional<Leader> drawLeader() {
        return leaderDeck.draw();
    }

    /**
     * Sucht den Besitzer eines ausgespielten Helden.
     *
     * @param hero Held
     * @return Spieler, in dessen Gruppe der Held liegt
     */
    public Optional<Player> findOwner(Hero hero) {
        return players.stream().filter(p -> p.getGroup().contains(hero)).findFirst();
    }

    /**
     * Liefert alle ausgespielten Helden der Gegner eines Spielers.
     *
     * @param player Spieler
     * @return Helden aller anderen Spieler
     */
    public List<Hero> getHeroesOfOpponents(Player player) {
        List<Hero> result = new ArrayList<>();
        for (Player p : players) {
            if (p != player) result.addAll(p.getGroup().getHeroes());
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Kartenbewegungen
    // ------------------------------------------------------------------

    /**
     * Zieht eine Karte vom Unterstützungsstapel auf die Hand.
     *
     * @param player ziehender Spieler
     * @return gezogene Karte; leer, wenn weder Stapel noch Ablagestapel Karten enthalten
     */
    public Optional<Card> drawSupportCard(Player player) {
        refillSupportIfNeeded();
        Optional<Card> card = supportDeck.draw();
        card.ifPresent(player.getHand()::add);
        refillSupportIfNeeded();
        return card;
    }

    /**
     * Verschiebt eine Handkarte ins Aktionsfeld.
     *
     * @param player Spieler
     * @param card   Karte
     * @return die tatsächlich bewegte Karteninstanz
     * @throws IllegalArgumentException wenn die Karte nicht auf der Hand ist
     */
    public Card moveHandToHover(Player player, Card card) {
        Card taken = player.getHand().take(card)
                           .orElseThrow(() -> new IllegalArgumentException(card + " ist nicht auf der Hand von " + player.getName()));
        player.getHover().put(taken);
        return taken;
    }

    /**
     * Legt eine Handkarte auf den Ablagestapel.
     *
     * @param player Spieler
     * @param card   Karte
     * @return die tatsächlich abgelegte Karteninstanz
     * @throws IllegalArgumentException wenn die Karte nicht auf der Hand ist
     */
    public Card moveHandToDiscard(Player player, Card card) {
        Card taken = player.getHand().take(card)
                           .orElseThrow(() -> new IllegalArgumentException(card + " ist nicht auf der Hand von " + player.getName()));
        discardPile.add(taken);
        return taken;
    }

    /**
     * Legt den Helden aus dem Aktionsfeld in die Gruppe.
     *
     * @param player Spieler
     * @return der Held
     * @throws IllegalStateException wenn im Aktionsfeld kein Held liegt oder die Gruppe voll ist
     */
    public Hero moveHoverToGroup(Player player) {
        Card card = player.getHover().getCard()
                          .orElseThrow(() -> new IllegalStateException("Aktionsfeld ist leer"));
        if (!(card instanceof Hero hero))
            throw new IllegalStateException("Im Aktionsfeld liegt kein Held: " + card);
        if (!player.getGroup().add(hero))
            throw new IllegalStateException("Die Gruppe von " + player.getName() + " ist voll");
        player.getHover().take();
        return hero;
    }

    /**
     * Legt die Karte aus dem Aktionsfeld auf den Ablagestapel.
     *
     * @param player Spieler
     * @return die abgelegte Karte; leer, wenn das Aktionsfeld leer war
     */
    public Optional<Card> moveHoverToDiscard(Player player) {
        Optional<Card> card = player.getHover().take();
        card.ifPresent(discardPile::add);
        return card;
    }

    /**
     * Legt einen Helden aus der Gruppe auf den Ablagestapel.
     *
     * @param player Besitzer
     * @param hero   Held
     * @throws IllegalArgumentException wenn der Held nicht in der Gruppe liegt
     */
    public void moveGroupToDiscard(Player player, Hero hero) {
        if (!player.getGroup().remove(hero))
            throw new IllegalArgumentException(hero + " liegt nicht in der Gruppe von " + player.getName());
        discardPile.add(hero);
    }

    /**
     * Nimmt die oberste Karte des Ablagestapels auf die Hand.
     *
     * @param player Spieler
     * @return aufgenommene Karte; leer, wenn der Ablagestapel leer ist
     */
    public Optional<Card> takeFromDiscard(Player player) {
        if (discardPile.isEmpty()) return Optional.empty();
        Card card = discardPile.removeLast();
        player.getHand().add(card);
        return Optional.of(card);
    }

    /**
     * Ein besiegtes Monster wandert zum Spieler; ein neues Monster wird aufgedeckt.
     * Der Belohnungsbonus des Monsters wird angewendet.
     *
     * @param player Sieger
     * @param monster besiegtes Monster
     * @return das nachgerückte Monster; leer, wenn der Monsterstapel leer ist
     */
    public Optional<Monster> defeatMonster(Player player, Monster monster) {
        if (!openMonsters.remove(monster))
            throw new IllegalArgumentException(monster + " liegt nicht offen aus");
        player.getMonsters().add(monster);
        if (monster.getReward() instanceof PassiveEffect reward)
            reward.applyTo(player);
        return refillMonsters();
    }

    /**
     * Mulligan: Alle Handkarten werden abgelegt und {@value #HAND_SIZE} neue gezogen.
     *
     * @param player Spieler
     * @return abgelegte und neu gezogene Karten
     */
    public Mulligan mulligan(Player player) {
        List<Card> old = player.getHand().clear();
        discardPile.addAll(old);
        List<Card> drawn = new ArrayList<>();
        for (int i = 0; i < HAND_SIZE; i++) {
            drawSupportCard(player).ifPresent(drawn::add);
        }
        return new Mulligan(old, drawn);
    }

    /**
     * Ergebnis eines Mulligans.
     *
     * @param discarded abgelegte Karten
     * @param drawn     neu gezogene Karten
     */
    public record Mulligan(List<Card> discarded, List<Card> drawn) {}

    // ------------------------------------------------------------------
    // Würfel & Siegbedingungen
    // ------------------------------------------------------------------

    /**
     * Würfelt mit zwei Würfeln.
     *
     * @param player würfelnder Spieler
     * @param bonus  passiver Bonus
     * @return Ergebnis
     */
    public DiceResult roll(Player player, int bonus) {
        return dice.roll(player.getName(), bonus);
    }

    /**
     * Prüft die Siegbedingungen: alle sechs Klassen (Anführer zählt mit) oder
     * drei besiegte Monster.
     *
     * @param player Spieler
     * @return {@code true}, wenn der Spieler gewonnen hat
     */
    public boolean hasWon(Player player) {
        return player.getClassTypes().size() >= CLASSES_TO_WIN
               || player.getMonsters().size() >= MONSTERS_TO_WIN;
    }

    // ------------------------------------------------------------------
    // Interne Verwaltung
    // ------------------------------------------------------------------

    private Optional<Monster> refillMonsters() {
        Monster last = null;
        while (openMonsters.hasFreeSlot() && !monsterDeck.isEmpty()) {
            last = monsterDeck.draw().orElseThrow();
            openMonsters.add(last);
        }
        return Optional.ofNullable(last);
    }

    private void refillSupportIfNeeded() {
        if (supportDeck.size() < REFILL_THRESHOLD)
            supportDeck.shuffleUnder(discardPile);
    }
}
