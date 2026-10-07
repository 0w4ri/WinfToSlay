package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Leader;
import pp.winf2slay.model.card.Monster;

/**
 * Öffentlich sichtbarer Zustand eines Spielers (ohne Handkarten).
 */
@Serializable
public class PlayerSnapshot {

    private String name;
    private boolean bot;
    private int handCount;
    private Card hover;
    private Hero[] group;
    private Monster[] monsters;
    private Leader leader;
    private int attackBonus;
    private int challengeBonus;
    private int heroBonus;

    private PlayerSnapshot() {
        // für @Serializable
    }

    /**
     * @param name           Spielername
     * @param bot            {@code true} für Bots
     * @param handCount      Anzahl der Handkarten
     * @param hover          Karte im Aktionsfeld oder {@code null}
     * @param group          Heldenplätze ({@code null} = frei)
     * @param monsters       besiegte Monster ({@code null} = frei)
     * @param leader         Anführer
     * @param attackBonus    Bonus auf Angriffswürfe
     * @param challengeBonus Bonus bei Herausforderungen
     * @param heroBonus      Bonus auf Heldeneffekte
     */
    public PlayerSnapshot(String name, boolean bot, int handCount, Card hover, Hero[] group, Monster[] monsters,
                          Leader leader, int attackBonus, int challengeBonus, int heroBonus) {
        this.name = name;
        this.bot = bot;
        this.handCount = handCount;
        this.hover = hover;
        this.group = group;
        this.monsters = monsters;
        this.leader = leader;
        this.attackBonus = attackBonus;
        this.challengeBonus = challengeBonus;
        this.heroBonus = heroBonus;
    }

    /**
     * @return Spielername
     */
    public String getName() {
        return name;
    }

    /**
     * @return {@code true} für Bots
     */
    public boolean isBot() {
        return bot;
    }

    /**
     * @return Anzahl der Handkarten
     */
    public int getHandCount() {
        return handCount;
    }

    /**
     * @return Karte im Aktionsfeld oder {@code null}
     */
    public Card getHover() {
        return hover;
    }

    /**
     * @return Heldenplätze ({@code null} = frei)
     */
    public Hero[] getGroup() {
        return group;
    }

    /**
     * @return besiegte Monster ({@code null} = frei)
     */
    public Monster[] getMonsters() {
        return monsters;
    }

    /**
     * @return Anführer
     */
    public Leader getLeader() {
        return leader;
    }

    /**
     * @return Bonus auf Angriffswürfe
     */
    public int getAttackBonus() {
        return attackBonus;
    }

    /**
     * @return Bonus bei Herausforderungen
     */
    public int getChallengeBonus() {
        return challengeBonus;
    }

    /**
     * @return Bonus auf Heldeneffekte
     */
    public int getHeroBonus() {
        return heroBonus;
    }

    /**
     * @return Anzahl der Helden in der Gruppe
     */
    public int getHeroCount() {
        int count = 0;
        for (Hero hero : group) {
            if (hero != null) count++;
        }
        return count;
    }

    /**
     * @return Anzahl der besiegten Monster
     */
    public int getMonsterCount() {
        int count = 0;
        for (Monster monster : monsters) {
            if (monster != null) count++;
        }
        return count;
    }
}
