package pp.winf2slay.model.card;

import com.jme3.network.serializing.Serializable;

import java.util.Objects;

/**
 * Basisklasse aller Karten von WinfToSlay.
 *
 * <p>Die Hierarchie ist {@code sealed}: Es gibt genau die sechs Kartentypen
 * {@link Hero}, {@link Spell}, {@link Challenge}, {@link Modification},
 * {@link Monster} und {@link Leader}. Dadurch lassen sich Karten mit
 * Pattern-Matching in {@code switch}-Ausdrücken vollständig unterscheiden.</p>
 *
 * <p>Jede Karte besitzt einen <em>technischen Namen</em> ({@link #getName()}),
 * der gleichzeitig der Dateiname des 3D-Modells ist (z. B. {@code Ranger_1}),
 * sowie einen <em>Anzeigenamen</em> ({@link #getDisplayName()}) für Texte in der
 * Oberfläche (z. B. {@code Falck}).</p>
 *
 * <p>Zwei Karten gelten als gleich, wenn sie vom selben Typ sind und denselben
 * technischen Namen besitzen. Mehrfach vorhandene Karten (z. B. Herausforderungen)
 * sind damit austauschbar – das Entfernen einer Karte aus einer Hand entfernt
 * genau ein Exemplar.</p>
 *
 * <p>Der parameterlose Konstruktor wird von der jME-Netzwerkserialisierung
 * ({@link Serializable}) benötigt.</p>
 */
@Serializable
public abstract sealed class Card permits Hero, Spell, Challenge, Modification, Monster, Leader {

    private String name;
    private String displayName;

    /**
     * Konstruktor für die Netzwerkserialisierung.
     */
    Card() {
        // für @Serializable
    }

    /**
     * Erzeugt eine Karte.
     *
     * @param name        technischer Name (Modell-Dateiname)
     * @param displayName Anzeigename; {@code null} übernimmt den technischen Namen
     */
    Card(String name, String displayName) {
        this.name = Objects.requireNonNull(name, "name");
        this.displayName = displayName != null ? displayName : name;
    }

    /**
     * Liefert den technischen Namen. Er entspricht dem Namen des 3D-Modells
     * im Ordner {@code cards/}.
     *
     * @return technischer Name, z. B. {@code Ranger_1}
     */
    public String getName() {
        return name;
    }

    /**
     * Liefert den Namen, unter dem die Karte in der Oberfläche angezeigt wird.
     *
     * @return Anzeigename, z. B. {@code Falck}
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Liefert eine kurze deutschsprachige Beschreibung des Kartentyps.
     *
     * @return Kartentyp, z. B. {@code Held}
     */
    public abstract String getTypeName();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != getClass()) return false;
        return name.equals(((Card) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + name + "]";
    }
}
