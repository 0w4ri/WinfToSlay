package pp.winf2slay.controller.server.bot;

/**
 * Wahrscheinlichkeiten für Würfe mit zwei sechsseitigen Würfeln.
 */
public final class Odds {

    /**
     * Häufigkeit jeder Augensumme 2–12 bei 36 möglichen Würfen.
     */
    private static final int[] WAYS = {0, 0, 1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1};

    private Odds() {
        // Utility-Klasse
    }

    /**
     * Wahrscheinlichkeit, dass ein Wurf (2W6 + Bonus) die Bedingung erfüllt.
     *
     * @param threshold  Schwellenwert
     * @param higherWins {@code true}: Wurf ≥ Schwelle; {@code false}: Wurf ≤ Schwelle
     * @param bonus      fester Bonus auf den Wurf
     * @return Wahrscheinlichkeit zwischen 0 und 1
     */
    public static double success(int threshold, boolean higherWins, int bonus) {
        int hits = 0;
        for (int sum = 2; sum <= 12; sum++) {
            int total = sum + bonus;
            boolean ok = higherWins ? total >= threshold : total <= threshold;
            if (ok) hits += WAYS[sum];
        }
        return hits / 36.0;
    }

    /**
     * Wahrscheinlichkeit, dass Spieler A mit Bonus {@code bonusA} strikt höher
     * würfelt als Spieler B mit Bonus {@code bonusB}.
     *
     * @param bonusA Bonus von A
     * @param bonusB Bonus von B
     * @return Wahrscheinlichkeit zwischen 0 und 1
     */
    public static double beats(int bonusA, int bonusB) {
        int wins = 0;
        for (int a = 2; a <= 12; a++) {
            for (int b = 2; b <= 12; b++) {
                if (a + bonusA > b + bonusB) wins += WAYS[a] * WAYS[b];
            }
        }
        return wins / 1296.0;
    }
}
