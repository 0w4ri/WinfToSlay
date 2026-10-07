package pp.winf2slay.model.field;

import pp.winf2slay.model.card.ClassType;
import pp.winf2slay.model.card.Hero;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Heldengruppe eines Spielers mit {@value #SIZE} festen Plätzen.
 *
 * <p>Die Plätze sind positionsstabil: Wird ein Held zerstört, bleibt sein Platz
 * frei, bis ein neuer Held gespielt wird. Die Oberfläche kann so jedem Helden
 * einen festen Slot auf dem Tisch zuordnen.</p>
 */
public class Group {

    /**
     * Maximale Anzahl Helden in einer Gruppe.
     */
    public static final int SIZE = 5;

    private final Hero[] slots = new Hero[SIZE];

    /**
     * @return Kopie der Slots; freie Plätze sind {@code null}
     */
    public Hero[] getSlots() {
        return slots.clone();
    }

    /**
     * @return alle Helden der Gruppe (ohne freie Plätze)
     */
    public List<Hero> getHeroes() {
        return new ArrayList<>(Arrays.stream(slots).filter(Objects::nonNull).toList());
    }

    /**
     * Legt einen Helden in den ersten freien Platz.
     *
     * @param hero Held
     * @return {@code true}, wenn ein Platz frei war
     */
    public boolean add(Hero hero) {
        Objects.requireNonNull(hero, "hero");
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                slots[i] = hero;
                return true;
            }
        }
        return false;
    }

    /**
     * Entfernt einen Helden.
     *
     * @param hero Held
     * @return {@code true}, wenn der Held in der Gruppe war
     */
    public boolean remove(Hero hero) {
        Objects.requireNonNull(hero, "hero");
        for (int i = 0; i < slots.length; i++) {
            if (hero.equals(slots[i])) {
                slots[i] = null;
                return true;
            }
        }
        return false;
    }

    /**
     * Sucht die in der Gruppe liegende Instanz eines Helden.
     *
     * @param hero gesuchter Held
     * @return Instanz aus der Gruppe; leer, wenn nicht vorhanden
     */
    public Optional<Hero> find(Hero hero) {
        return Arrays.stream(slots).filter(h -> h != null && h.equals(hero)).findFirst();
    }

    /**
     * @param hero Held
     * @return {@code true}, wenn der Held in der Gruppe liegt
     */
    public boolean contains(Hero hero) {
        return find(hero).isPresent();
    }

    /**
     * @return {@code true}, wenn mindestens ein Platz frei ist
     */
    public boolean hasFreeSlot() {
        return size() < SIZE;
    }

    /**
     * @return Anzahl der Helden
     */
    public int size() {
        return (int) Arrays.stream(slots).filter(Objects::nonNull).count();
    }

    /**
     * @return {@code true}, wenn die Gruppe keinen Helden enthält
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * @return Menge der in der Gruppe vertretenen Klassen
     */
    public Set<ClassType> getClassTypes() {
        Set<ClassType> types = EnumSet.noneOf(ClassType.class);
        for (Hero hero : slots) {
            if (hero != null) types.add(hero.getClassType());
        }
        return types;
    }
}
