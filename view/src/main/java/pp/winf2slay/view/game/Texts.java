package pp.winf2slay.view.game;

import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.model.Action;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Challenge;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Leader;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.card.Spell;
import pp.winf2slay.model.die.DiceResult;

/**
 * Deutschsprachige Texte rund um Karten und Würfe.
 */
public final class Texts {

    private Texts() {
        // Utility-Klasse
    }

    /**
     * @param card Karte
     * @return Typzeile, z. B. „Held · Waldläufer“
     */
    public static String typeLine(Card card) {
        return switch (card) {
            case Hero h -> "Held · " + h.getClassType().getDisplayName();
            case Leader l -> "Anführer · " + l.getClassType().getDisplayName();
            case Monster m -> "Monster";
            case Spell s -> "Zauber";
            case Modification m -> "Modifikation";
            case Challenge c -> "Herausforderung";
        };
    }

    /**
     * @param card Karte
     * @return ausführliche Beschreibung für die Großansicht
     */
    public static String describe(Card card) {
        return switch (card) {
            case Hero h -> "Effekt bei einem Wurf von " + h.getThreshold() + " oder mehr:\n" + h.getEffect().describe()
                           + "\n\nAusspielen kostet " + Action.PLAY_CARD.getCost() + " AP, danach wird sofort gewürfelt."
                           + " Später kann der Effekt einmal pro Zug für " + Action.ACTIVATE_HERO.getCost()
                           + " AP ausgelöst werden.";
            case Leader l -> "Passiv: " + l.getEffect().describe() + "\n\nDer Anführer zählt als Klasse "
                             + l.getClassType().getDisplayName() + " für den Sieg.";
            case Monster m -> "Besiegt bei einem Wurf von " + goal(m.getThreshold(), m.isHigherWins()) + ".\n"
                              + "Belohnung: " + m.getReward().describe() + "\n"
                              + "Bei Misserfolg: " + m.getPenalty().describe()
                              + "\n\nAngriff kostet " + Action.ATTACK_MONSTER.getCost() + " AP und erfordert mindestens "
                              + Action.HEROES_TO_ATTACK + " Helden.";
            case Spell s -> s.getEffect().describe() + "\n\nAusspielen kostet " + Action.PLAY_CARD.getCost()
                            + " AP. Der Zauber kann herausgefordert werden.";
            case Modification m -> "Verändert einen Würfelwurf um " + signed(m.getDelta()) + ".\n\n"
                                   + "Wird gespielt, nachdem jemand gewürfelt hat – auch im Zug anderer Spieler.";
            case Challenge c -> "Fordere einen gerade gespielten Helden oder Zauber heraus.\n\n"
                                + "Beide würfeln; wer höher würfelt, gewinnt. Verliert der ausspielende Spieler, "
                                + "landet seine Karte auf dem Ablagestapel.";
        };
    }

    /**
     * @param card Karte
     * @return kurzer Hinweis, warum eine Karte nicht direkt gespielt werden kann
     */
    public static String reactionOnly(Card card) {
        return switch (card) {
            case Modification m -> "Modifikationen spielst du, nachdem gewürfelt wurde.";
            case Challenge c -> "Herausforderungen spielst du, wenn ein Gegner eine Karte ausspielt.";
            default -> "Diese Karte kann gerade nicht gespielt werden.";
        };
    }

    /**
     * @param threshold  Zielwert
     * @param higherWins {@code true}, wenn der Wurf mindestens den Zielwert erreichen muss
     * @return z. B. „8 oder mehr“
     */
    public static String goal(int threshold, boolean higherWins) {
        return threshold + (higherWins ? " oder mehr" : " oder weniger");
    }

    /**
     * @param purpose Zweck eines Wurfs
     * @return Anzeigename
     */
    public static String purpose(RollPurpose purpose) {
        return switch (purpose) {
            case HERO_EFFECT -> "Heldeneffekt";
            case MONSTER_ATTACK -> "Monsterangriff";
            case CHALLENGE -> "Herausforderung";
        };
    }

    /**
     * @param value Zahl
     * @return Zahl mit Vorzeichen, z. B. „+2“ oder „–4“
     */
    public static String signed(int value) {
        return value >= 0 ? "+" + value : "–" + Math.abs(value); // Halbgeviertstrich: U+2212 fehlt in den Schriften
    }

    /**
     * @param result Wurf
     * @return Aufschlüsselung, z. B. „3 + 5 + 1 = 9“
     */
    public static String breakdown(DiceResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(result.getDie1()).append(" + ").append(result.getDie2());
        if (result.getBonus() != 0) sb.append("  ").append(signed(result.getBonus())).append(" Bonus");
        if (result.getModification() != 0) sb.append("  ").append(signed(result.getModification())).append(" Mod.");
        return sb.toString();
    }
}
