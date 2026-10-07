package pp.winf2slay.controller.server.bot;

import pp.winf2slay.controller.server.TurnInfo;
import pp.winf2slay.model.Action;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.ClassType;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Monster;

import java.util.List;
import java.util.Set;

/**
 * Sicht eines Bots auf das Spiel. Bots nutzen nur Informationen, die auch ein
 * Mensch hätte: die eigenen Handkarten und den offen sichtbaren Tisch.
 *
 * @param game   Spiel
 * @param me     gesteuerter Spieler
 * @param turn   Zug-Informationen (nur aussagekräftig, wenn der Bot am Zug ist)
 * @param active Spieler am Zug
 */
public record BotContext(Game game, Player me, TurnInfo turn, Player active) {

    /**
     * @return {@code true}, wenn der Bot am Zug ist
     */
    public boolean myTurn() {
        return me == active;
    }

    /**
     * @return verbleibende Aktionspunkte
     */
    public int actionPoints() {
        return turn.getActionPoints();
    }

    /**
     * @param action Aktion
     * @return {@code true}, wenn der Bot sich die Aktion leisten kann
     */
    public boolean canAfford(Action action) {
        return myTurn() && turn.canAfford(action);
    }

    /**
     * @return eigene Handkarten
     */
    public List<Card> hand() {
        return me.getHand().getCards();
    }

    /**
     * @return Gegner in Sitzreihenfolge
     */
    public List<Player> opponents() {
        return game.getPlayers().stream().filter(p -> p != me).toList();
    }

    /**
     * @return eigene vertretene Klassen (inklusive Anführer)
     */
    public Set<ClassType> myClasses() {
        return me.getClassTypes();
    }

    /**
     * @return offen ausliegende Monster
     */
    public List<Monster> openMonsters() {
        return game.getOpenMonsters().getMonsters();
    }

    /**
     * Grobe Einschätzung, wie nah ein Spieler am Sieg ist (0 = weit weg, 1 = gewonnen).
     *
     * @param player Spieler
     * @return Fortschritt zwischen 0 und 1
     */
    public double progress(Player player) {
        double classes = player.getClassTypes().size() / (double) Game.CLASSES_TO_WIN;
        double monsters = player.getMonsters().size() / (double) Game.MONSTERS_TO_WIN;
        return Math.max(classes, monsters);
    }

    /**
     * @return Gegner mit dem größten Fortschritt
     */
    public Player strongestOpponent() {
        Player best = null;
        double bestScore = -1;
        for (Player p : opponents()) {
            double score = progress(p) + 0.05 * p.getGroup().size();
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    /**
     * @param hero Held
     * @return Besitzer des Helden oder {@code null}
     */
    public Player ownerOf(Hero hero) {
        return game.findOwner(hero).orElse(null);
    }
}
