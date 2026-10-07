package pp.winf2slay.view.game;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import pp.winf2slay.model.field.Group;

/**
 * Berechnet alle Positionen auf dem Spieltisch.
 *
 * <p>Der eigene Platz liegt immer vorne (zur Kamera hin). Die Gegner verteilen
 * sich auf einem Ellipsenbogen über die hintere Tischhälfte. Jeder Platz hat ein
 * lokales Koordinatensystem, dessen −z-Achse zur Tischmitte zeigt.</p>
 */
public class BoardLayout {

    /** Höhe, in der Karten auf dem Tisch liegen. */
    public static final float CARD_Y = 0.08f;

    private static final float RADIUS_X = 45f;
    private static final float RADIUS_Z = 36f;
    private static final float OWN_SEAT_Z = 31f;
    private static final float OPPONENT_ARC_START = 78f;
    private static final float OPPONENT_ARC = 204f;

    /** Abstand der Heldenplätze. */
    public static final float HERO_SPACING = 6.9f;

    private final int seats;

    /**
     * @param seats Anzahl der Spieler (2–6)
     */
    public BoardLayout(int seats) {
        this.seats = Math.max(1, seats);
    }

    /**
     * @return Anzahl der Plätze
     */
    public int getSeats() {
        return seats;
    }

    /**
     * @param seat Platz (0 = eigener Platz)
     * @return Lage des Platzes auf dem Tisch
     */
    public Transform seat(int seat) {
        float angle;
        if (seat == 0) {
            angle = 0;
        }
        else {
            int opponents = seats - 1;
            angle = OPPONENT_ARC_START + OPPONENT_ARC * (seat - 0.5f) / opponents;
        }
        float rad = angle * FastMath.DEG_TO_RAD;
        float scale = seat == 0 ? 1f : 0.92f;
        Vector3f pos = seat == 0 ? new Vector3f(0, 0, OWN_SEAT_Z)
                                 : new Vector3f(RADIUS_X * FastMath.sin(rad), 0, RADIUS_Z * FastMath.cos(rad));
        float yaw = FastMath.atan2(pos.x, pos.z);
        Transform t = new Transform(pos, new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y));
        t.setScale(scale);
        return t;
    }

    private Transform local(int seat, float x, float y, float z) {
        Transform s = seat(seat);
        Vector3f p = s.transformVector(new Vector3f(x, y, z), null);
        Transform t = new Transform(p, s.getRotation().clone());
        t.setScale(s.getScale());
        return t;
    }

    /**
     * @param seat Platz
     * @param slot Heldenplatz 0–4
     * @return Lage einer Heldenkarte
     */
    public Transform hero(int seat, int slot) {
        float x = (slot - (Group.SIZE - 1) / 2f) * HERO_SPACING;
        return local(seat, x, CARD_Y, -1.5f);
    }

    /**
     * @param seat Platz
     * @return Lage des Anführers
     */
    public Transform leader(int seat) {
        return local(seat, -22.6f, CARD_Y, -0.6f);
    }

    /**
     * @param seat  Platz
     * @param index Nummer des besiegten Monsters
     * @return Lage einer Monster-Trophäe
     */
    public Transform trophy(int seat, int index) {
        Transform t = local(seat, 22.6f + index * 1.3f, CARD_Y + index * 0.08f, -0.6f + index * 1.1f);
        t.getRotation().multLocal(new Quaternion().fromAngleAxis(-0.06f * index, Vector3f.UNIT_Y));
        return t;
    }

    /**
     * @param seat Platz
     * @return Lage einer gerade gespielten Karte (wartet auf Herausforderung)
     */
    public Transform hover(int seat) {
        return local(seat, 0, 2.2f, -13.5f);
    }

    /**
     * @param seat  Platz
     * @param index Kartennummer
     * @param count Anzahl der Handkarten
     * @return Lage einer verdeckten Handkarte eines Gegners
     */
    public Transform handCard(int seat, int index, int count) {
        float spread = Math.min(2.4f, 16f / Math.max(1, count));
        float x = (index - (count - 1) / 2f) * spread;
        Transform t = local(seat, x, 0.6f + index * 0.03f, 10.5f);
        float tilt = (index - (count - 1) / 2f) * 0.06f;
        t.getRotation().multLocal(new Quaternion().fromAngles(-0.55f, -tilt, 0));
        return t;
    }

    /**
     * @param seat Platz
     * @return Mitte des Platzes (für Namensschilder und Effekte)
     */
    public Vector3f seatCenter(int seat) {
        return seat(seat).getTranslation().clone();
    }

    // ------------------------------------------------------------------
    // Tischmitte
    // ------------------------------------------------------------------

    /**
     * @param index Monsterplatz 0–2
     * @return Lage eines offenen Monsters
     */
    public Transform openMonster(int index) {
        return flat(new Vector3f((index - 1) * 9.6f, CARD_Y, -3f));
    }

    /**
     * @return Lage des Monsterstapels
     */
    public Transform monsterDeck() {
        return flat(new Vector3f(-21.5f, CARD_Y, -3f));
    }

    /**
     * @return Lage des Nachziehstapels
     */
    public Transform supportDeck() {
        return flat(new Vector3f(20.5f, CARD_Y, -3.5f));
    }

    /**
     * @return Lage des Ablagestapels
     */
    public Transform discardPile() {
        return flat(new Vector3f(29f, CARD_Y, -3.5f));
    }

    /**
     * @return Mitte der Würfelfläche
     */
    public Vector3f diceArea() {
        return new Vector3f(0, 0, 8.5f);
    }

    /**
     * @return Punkt in der Tischmitte über den Karten (für große Effekte)
     */
    public Vector3f center() {
        return new Vector3f(0, 0, 0);
    }

    private static Transform flat(Vector3f pos) {
        return new Transform(pos, new Quaternion());
    }
}
