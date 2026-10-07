package pp.winf2slay.model.die;

import java.util.Objects;
import java.util.Random;

/**
 * Sechsseitiger Würfel (W6).
 *
 * <p>Der Zufallsgenerator wird von außen übergeben; mit einem festen Seed sind
 * Würfe reproduzierbar (wichtig für Tests).</p>
 */
public class Die {

    /**
     * Anzahl der Seiten.
     */
    public static final int SIDES = 6;

    private final Random rng;

    /**
     * @param rng Zufallsgenerator (nicht {@code null})
     */
    public Die(Random rng) {
        this.rng = Objects.requireNonNull(rng, "rng");
    }

    /**
     * @return Wurfergebnis zwischen 1 und 6
     */
    public int roll() {
        return rng.nextInt(SIDES) + 1;
    }
}
