package pp.winf2slay.controller.message;

/**
 * Grund einer Kartenbewegung. Damit kann die Oberfläche für jede Ursache eine
 * eigene Animation abspielen (z. B. Mörser, Bombe, Helikopter).
 */
public enum MoveCause {
    /** Aktion „Karte ziehen“ */
    DRAW,
    /** Karte wird von der Hand ins Aktionsfeld gespielt */
    PLAY,
    /** Ausgespielte Karte wurde nicht (erfolgreich) herausgefordert */
    PLAY_SUCCEEDED,
    /** Ausgespielte Karte hat die Herausforderung verloren */
    PLAY_FAILED,
    /** Eine Herausforderungskarte wird gespielt */
    CHALLENGE,
    /** Eine Modifikationskarte wird gespielt */
    MODIFICATION,
    /** Effekt „Ziehe Karten“ */
    EFFECT_DRAW,
    /** Effekt „Nimm Karten vom Ablagestapel“ */
    EFFECT_DRAW_DISCARD,
    /** Effekt „Zerstöre einen ausgewählten Helden“ */
    EFFECT_DESTROY,
    /** Effekt „Zerstöre zufällige Helden“ (Mörser) */
    EFFECT_DESTROY_RANDOM,
    /** Effekt „Zerstöre alle Helden eines Spielers“ (Helikopter) */
    EFFECT_DESTROY_PARTY,
    /** Effekt „Zerstöre alle Helden“ (Bombe) */
    EFFECT_DESTROY_ALL,
    /** Effekt „Jeder wirft Karten ab“ */
    EFFECT_DISCARD,
    /** Ein Monster wurde besiegt */
    MONSTER_DEFEATED,
    /** Strafe eines verlorenen Monsterkampfs („Opfere einen Helden“) */
    MONSTER_PENALTY
}
