package pp.winf2slay.model;

/**
 * Aktionen, die der aktive Spieler in seinem Zug ausführen kann, mit ihren
 * Kosten in Aktionspunkten.
 */
public enum Action {
    /** Eine Karte vom Unterstützungsstapel ziehen */
    DRAW_CARD(1),
    /** Einen Helden oder Zauber ausspielen */
    PLAY_CARD(1),
    /** Den Effekt eines eigenen Helden auslösen (einmal pro Zug und Held) */
    ACTIVATE_HERO(1),
    /** Ein offenes Monster angreifen (mindestens zwei Helden nötig) */
    ATTACK_MONSTER(2),
    /** Alle Handkarten abwerfen und fünf neue ziehen */
    MULLIGAN(3);

    /**
     * Aktionspunkte zu Beginn jedes Zuges.
     */
    public static final int POINTS_PER_TURN = 3;

    /**
     * Mindestanzahl Helden für einen Monsterangriff.
     */
    public static final int HEROES_TO_ATTACK = 2;

    private final int cost;

    Action(int cost) {
        this.cost = cost;
    }

    /**
     * @return Kosten in Aktionspunkten
     */
    public int getCost() {
        return cost;
    }
}
