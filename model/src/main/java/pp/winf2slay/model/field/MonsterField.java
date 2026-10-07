package pp.winf2slay.model.field;

import pp.winf2slay.model.card.Monster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Drei Monsterplätze – entweder die offen ausliegenden Monster in der
 * Tischmitte oder die von einem Spieler besiegten Monster.
 */
public class MonsterField {

    /**
     * Anzahl der Plätze.
     */
    public static final int SIZE = 3;

    private final Monster[] slots = new Monster[SIZE];

    /**
     * @return Kopie der Plätze; freie Plätze sind {@code null}
     */
    public Monster[] getSlots() {
        return slots.clone();
    }

    /**
     * @return alle Monster (ohne freie Plätze)
     */
    public List<Monster> getMonsters() {
        return new ArrayList<>(Arrays.stream(slots).filter(Objects::nonNull).toList());
    }

    /**
     * Legt ein Monster in den ersten freien Platz.
     *
     * @param monster Monster
     * @return {@code true}, wenn ein Platz frei war
     */
    public boolean add(Monster monster) {
        Objects.requireNonNull(monster, "monster");
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                slots[i] = monster;
                return true;
            }
        }
        return false;
    }

    /**
     * Entfernt ein Monster.
     *
     * @param monster Monster
     * @return {@code true}, wenn das Monster vorhanden war
     */
    public boolean remove(Monster monster) {
        Objects.requireNonNull(monster, "monster");
        for (int i = 0; i < slots.length; i++) {
            if (monster.equals(slots[i])) {
                slots[i] = null;
                return true;
            }
        }
        return false;
    }

    /**
     * Sucht die abgelegte Instanz eines Monsters.
     *
     * @param monster gesuchtes Monster
     * @return Instanz; leer, wenn nicht vorhanden
     */
    public Optional<Monster> find(Monster monster) {
        return Arrays.stream(slots).filter(m -> m != null && m.equals(monster)).findFirst();
    }

    /**
     * @return {@code true}, wenn mindestens ein Platz frei ist
     */
    public boolean hasFreeSlot() {
        return size() < SIZE;
    }

    /**
     * @return Anzahl der Monster
     */
    public int size() {
        return (int) Arrays.stream(slots).filter(Objects::nonNull).count();
    }
}
