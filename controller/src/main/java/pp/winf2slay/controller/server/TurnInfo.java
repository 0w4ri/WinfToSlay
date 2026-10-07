package pp.winf2slay.controller.server;

import pp.winf2slay.model.Action;
import pp.winf2slay.model.card.Hero;

import java.util.HashSet;
import java.util.Set;

/**
 * Zustand des laufenden Zuges: verbleibende Aktionspunkte und die Helden,
 * deren Effekt in diesem Zug bereits verwendet wurde.
 *
 * <p>Der Server prüft damit jede Aktion – auch die der Bots – unabhängig vom
 * Client.</p>
 */
public class TurnInfo {

    private int actionPoints;
    private final Set<String> usedHeroes = new HashSet<>();

    /**
     * Setzt den Zug zurück (volle Aktionspunkte, keine verbrauchten Helden).
     */
    public void reset() {
        actionPoints = Action.POINTS_PER_TURN;
        usedHeroes.clear();
    }

    /**
     * @return verbleibende Aktionspunkte
     */
    public int getActionPoints() {
        return actionPoints;
    }

    /**
     * @param action geplante Aktion
     * @return {@code true}, wenn genug Aktionspunkte vorhanden sind
     */
    public boolean canAfford(Action action) {
        return actionPoints >= action.getCost();
    }

    /**
     * Zieht die Kosten einer Aktion ab.
     *
     * @param action ausgeführte Aktion
     * @throws IllegalStateException wenn nicht genug Aktionspunkte vorhanden sind
     */
    public void spend(Action action) {
        if (!canAfford(action))
            throw new IllegalStateException("Zu wenige Aktionspunkte für " + action);
        actionPoints -= action.getCost();
    }

    /**
     * @param hero Held
     * @return {@code true}, wenn sein Effekt in diesem Zug schon genutzt wurde
     */
    public boolean isUsed(Hero hero) {
        return usedHeroes.contains(hero.getName());
    }

    /**
     * Vermerkt, dass der Effekt eines Helden in diesem Zug genutzt wurde.
     *
     * @param hero Held
     */
    public void markUsed(Hero hero) {
        usedHeroes.add(hero.getName());
    }
}
