package pp.winf2slay.controller.server;

import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Hero;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Optional;

/**
 * Entwicklerwerkzeug für reproduzierbare Oberflächentests. Wird nur aktiv, wenn beim
 * Start entsprechende Systemeigenschaften gesetzt sind:
 * <ul>
 *     <li>{@code -Dwinf.testhand=Spell_1,Hero_...}: Der erste Mensch erhält diese Karten
 *     (technische Namen) aus dem Nachziehstapel statt seiner zufälligen Starthand.</li>
 *     <li>{@code -Dwinf.testgroups=n}: Jeder Spieler beginnt mit {@code n} Helden aus dem
 *     Nachziehstapel in seiner Gruppe (damit Zerstörungszauber Ziele haben).</li>
 * </ul>
 * Karten werden nur verschoben, nie erzeugt – die Gesamtzahl bleibt gleich.
 */
public final class TestSetup {

    private static final Logger LOGGER = System.getLogger(TestSetup.class.getName());

    private TestSetup() {
        // Utility-Klasse
    }

    /**
     * Wendet den Testaufbau an (ohne gesetzte Eigenschaften: nichts).
     *
     * @param game    Spiel
     * @param players Spieler in Sitzreihenfolge (Starthände bereits ausgeteilt)
     */
    public static void apply(Game game, List<Player> players) {
        int groups = Integer.getInteger("winf.testgroups", 0);
        if (groups > 0) {
            for (Player p : players) {
                for (int i = 0; i < groups; i++) {
                    Optional<Card> hero = game.getSupportDeck().takeFirst(c -> c instanceof Hero);
                    hero.ifPresent(h -> p.getGroup().add((Hero) h));
                }
            }
            LOGGER.log(Level.WARNING, "Testaufbau: {0} Helden je Gruppe", groups);
        }
        String hand = System.getProperty("winf.testhand");
        if (hand == null || hand.isBlank()) return;
        Player human = players.stream().filter(p -> !p.isBot()).findFirst().orElse(null);
        if (human == null) return;
        for (Card c : human.getHand().clear()) game.getSupportDeck().putToBottom(c);
        for (String name : hand.split(",")) {
            String wanted = name.strip();
            Optional<Card> card = game.getSupportDeck().takeFirst(c -> c.getName().equals(wanted));
            if (card.isEmpty()) card = takeFromOtherHands(game, players, human, wanted);
            card.ifPresentOrElse(c -> human.getHand().add(c),
                                 () -> LOGGER.log(Level.WARNING, "Testaufbau: Karte {0} nicht gefunden", wanted));
        }
        while (human.getHand().size() < Game.HAND_SIZE) {
            if (game.drawSupportCard(human).isEmpty()) break;
        }
        LOGGER.log(Level.WARNING, "Testaufbau: Starthand von {0} = {1}", human.getName(), hand);
    }

    /**
     * Holt eine Karte aus der Hand eines anderen Spielers; dieser bekommt Ersatz vom Stapel.
     */
    private static Optional<Card> takeFromOtherHands(Game game, List<Player> players, Player human, String name) {
        for (Player p : players) {
            if (p == human) continue;
            for (Card c : p.getHand().getCards()) {
                if (c.getName().equals(name)) {
                    Optional<Card> taken = p.getHand().take(c);
                    game.drawSupportCard(p);
                    return taken;
                }
            }
        }
        return Optional.empty();
    }
}
