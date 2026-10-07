package pp.winf2slay.view.audio;

/**
 * Alle Soundeffekte des Spiels mit Datei und Grundlautstärke.
 */
public enum Sfx {
    /** Klick auf eine Schaltfläche. */
    CLICK("click", 0.6f),
    /** Maus über einer Schaltfläche. */
    HOVER("hover", 0.25f),
    /** Karte fliegt. */
    WHOOSH("whoosh", 0.45f),
    /** Karte wird abgelegt. */
    CARD_PLACE("card_place", 0.8f),
    /** Karte wird umgedreht. */
    CARD_FLIP("card_flip", 0.7f),
    /** Karte wird bewegt (Originalsound). */
    CARD_MOVE("cardmove", 0.6f),
    /** Karten werden gemischt/ausgeteilt. */
    SHUFFLE("shuffle", 0.7f),
    /** Würfel rollen. */
    DICE("dice_roll", 0.8f),
    /** Explosion. */
    EXPLOSION("explosion", 0.85f),
    /** Große Explosion (Bombe). */
    EXPLOSION_BIG("explosion_big", 1f),
    /** Pfeifen einer Granate. */
    WHISTLE("whistle", 0.55f),
    /** Blitz. */
    THUNDER("thunder", 0.8f),
    /** Magie (Heldeneffekt). */
    MAGIC("magic", 0.55f),
    /** Funkeln. */
    SPARKLE("sparkle", 0.5f),
    /** Schwerter (Herausforderung). */
    SWORDS("sword_clash", 0.65f),
    /** Erfolg. */
    SUCCESS("success", 0.6f),
    /** Fehlschlag. */
    FAIL("fail", 0.6f),
    /** Feuer. */
    BURN("burn", 0.6f),
    /** Einblendung eines Banners. */
    BANNER("banner", 0.5f),
    /** Aktionspunkt ausgegeben. */
    AP("ap_spend", 0.4f),
    /** Hubschrauber. */
    HELI("heli", 0.7f),
    /** Minigun. */
    MINIGUN("minigun", 0.6f),
    /** Monster greift an. */
    MONSTER_ATTACK("monster_attack", 0.7f),
    /** Monster besiegt. */
    MONSTER_DEFEATED("monster_defeated", 0.8f),
    /** Angriff verfehlt. */
    MONSTER_MISS("monster_miss", 0.7f),
    /** Eigener Zug beginnt. */
    MY_TURN("my_turn", 0.7f),
    /** Etwas geht schief (Metallrohr). */
    PIPE("pipefall", 0.6f),
    /** Spiel gewonnen. */
    WIN("win_game", 0.85f),
    /** Spiel verloren. */
    LOSE("lose_game", 0.75f),
    /** Uhr tickt (Wartezeit). */
    TICK("clock_tick", 0.35f);

    private final String file;
    private final float volume;

    Sfx(String file, float volume) {
        this.file = file;
        this.volume = volume;
    }

    /**
     * @return Asset-Pfad
     */
    public String getPath() {
        return "sounds/" + file + ".ogg";
    }

    /**
     * @return Grundlautstärke 0..1
     */
    public float getVolume() {
        return volume;
    }
}
