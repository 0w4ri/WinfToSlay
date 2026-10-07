package pp.winf2slay.model;

import pp.winf2slay.model.card.ClassType;
import pp.winf2slay.model.card.Leader;
import pp.winf2slay.model.effect.PassiveEffect;
import pp.winf2slay.model.field.Group;
import pp.winf2slay.model.field.Hand;
import pp.winf2slay.model.field.Hover;
import pp.winf2slay.model.field.MonsterField;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Ein Spieler mit Handkarten, Heldengruppe, Aktionsfeld, besiegten Monstern,
 * Anführer und dauerhaften Würfelboni.
 */
public class Player {

    private final int id;
    private String name;
    private boolean bot;
    private Leader leader;

    private int attackBonus;
    private int challengeBonus;
    private int heroBonus;

    private final Hand hand = new Hand();
    private final Group group = new Group();
    private final Hover hover = new Hover();
    private final MonsterField monsters = new MonsterField();

    /**
     * Erzeugt einen Spieler.
     *
     * @param id   eindeutige ID (Verbindungs-ID; Bots erhalten negative IDs)
     * @param name Anzeigename
     * @param bot  {@code true}, wenn der Spieler vom Computer gesteuert wird
     */
    public Player(int id, String name, boolean bot) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.bot = bot;
    }

    /**
     * Erzeugt einen menschlichen Spieler.
     *
     * @param id   eindeutige ID
     * @param name Anzeigename
     */
    public Player(int id, String name) {
        this(id, name, false);
    }

    /**
     * @return eindeutige ID
     */
    public int getId() {
        return id;
    }

    /**
     * @return Anzeigename
     */
    public String getName() {
        return name;
    }

    /**
     * @param name neuer Anzeigename
     */
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    /**
     * @return {@code true}, wenn der Spieler ein Bot ist
     */
    public boolean isBot() {
        return bot;
    }

    /**
     * Übergibt den Spieler an einen Bot, z. B. wenn ein Mitspieler die
     * Verbindung verliert.
     */
    public void replaceByBot() {
        this.bot = true;
    }

    /**
     * @return Anführer; {@code null}, solange das Spiel nicht begonnen hat
     */
    public Leader getLeader() {
        return leader;
    }

    /**
     * Weist den Anführer zu und wendet dessen passiven Effekt an.
     *
     * @param leader Anführer
     * @throws IllegalStateException wenn bereits ein Anführer zugewiesen ist
     */
    public void assignLeader(Leader leader) {
        if (this.leader != null)
            throw new IllegalStateException(name + " hat bereits einen Anführer");
        this.leader = Objects.requireNonNull(leader, "leader");
        if (leader.getEffect() instanceof PassiveEffect passive)
            passive.applyTo(this);
    }

    // ------------------------------------------------------------------
    // Würfelboni
    // ------------------------------------------------------------------

    /**
     * @return Bonus auf Angriffswürfe gegen Monster
     */
    public int getAttackBonus() {
        return attackBonus;
    }

    /**
     * @return Bonus auf Würfe bei Herausforderungen
     */
    public int getChallengeBonus() {
        return challengeBonus;
    }

    /**
     * @return Bonus auf Würfe zum Auslösen von Heldeneffekten
     */
    public int getHeroBonus() {
        return heroBonus;
    }

    /**
     * @param bonus zusätzlicher Angriffsbonus (nicht negativ)
     */
    public void addAttackBonus(int bonus) {
        attackBonus += requireNonNegative(bonus);
    }

    /**
     * @param bonus zusätzlicher Herausforderungsbonus (nicht negativ)
     */
    public void addChallengeBonus(int bonus) {
        challengeBonus += requireNonNegative(bonus);
    }

    /**
     * @param bonus zusätzlicher Heldenbonus (nicht negativ)
     */
    public void addHeroBonus(int bonus) {
        heroBonus += requireNonNegative(bonus);
    }

    private static int requireNonNegative(int bonus) {
        if (bonus < 0)
            throw new IllegalArgumentException("Der Bonus darf nicht negativ sein: " + bonus);
        return bonus;
    }

    // ------------------------------------------------------------------
    // Spielbereiche
    // ------------------------------------------------------------------

    /**
     * @return Handkarten
     */
    public Hand getHand() {
        return hand;
    }

    /**
     * @return Heldengruppe
     */
    public Group getGroup() {
        return group;
    }

    /**
     * @return Aktionsfeld
     */
    public Hover getHover() {
        return hover;
    }

    /**
     * @return besiegte Monster
     */
    public MonsterField getMonsters() {
        return monsters;
    }

    /**
     * Liefert alle Klassen, die der Spieler über Anführer und Helden vorweisen kann.
     *
     * @return vertretene Klassen
     */
    public Set<ClassType> getClassTypes() {
        Set<ClassType> types = EnumSet.noneOf(ClassType.class);
        if (leader != null) types.add(leader.getClassType());
        types.addAll(group.getClassTypes());
        return types;
    }

    @Override
    public String toString() {
        return "Player[" + id + ", " + name + (bot ? ", Bot" : "") + "]";
    }
}
