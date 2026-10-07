package pp.winf2slay.controller.message;

/**
 * Anlass eines Würfelwurfs.
 */
public enum RollPurpose {
    /** Wurf, um einen Heldeneffekt auszulösen */
    HERO_EFFECT,
    /** Angriffswurf gegen ein Monster */
    MONSTER_ATTACK,
    /** Wurf im Rahmen einer Herausforderung */
    CHALLENGE
}
