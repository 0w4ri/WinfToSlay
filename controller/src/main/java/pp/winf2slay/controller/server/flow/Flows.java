package pp.winf2slay.controller.server.flow;

import pp.winf2slay.controller.server.ServerGameLogic;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * Hilfsfunktionen für die Spielabläufe.
 */
final class Flows {

    private Flows() {
        // Utility-Klasse
    }

    /**
     * Liefert alle Spieler mit mindestens einer Handkarte des gesuchten Typs –
     * in Sitzreihenfolge, beginnend bei {@code first}.
     *
     * @param logic    Serverlogik
     * @param cardType gesuchter Kartentyp
     * @param first    Spieler, bei dem die Reihenfolge beginnt
     * @return passende Spieler
     */
    static List<Player> playersWith(ServerGameLogic logic, Class<? extends Card> cardType, Player first) {
        List<Player> all = logic.getPlayers();
        int start = Math.max(0, all.indexOf(first));
        List<Player> result = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            Player p = all.get((start + i) % all.size());
            if (p.getHand().getCards().stream().anyMatch(cardType::isInstance)) result.add(p);
        }
        return result;
    }
}
