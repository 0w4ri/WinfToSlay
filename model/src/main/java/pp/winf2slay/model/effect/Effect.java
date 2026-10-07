package pp.winf2slay.model.effect;

/**
 * Effekt einer Karte.
 *
 * <p>Effekte sind unveränderliche Beschreibungen (sie speichern keinen
 * Spielzustand) und werden zusammen mit den Karten über das Netzwerk
 * übertragen. Die eigentliche Wirkung stellen die einzelnen Effektklassen als
 * typisierte Methoden bereit, die vom Server Schritt für Schritt aufgerufen
 * werden – z. B. muss bei {@link DestroyHero} vor jeder Zerstörung ein Ziel
 * ausgewählt werden.</p>
 *
 * <p>Die Menge der Effekte ist {@code sealed}, damit Server, Bots und
 * Oberfläche alle Varianten vollständig per Pattern-Matching behandeln.</p>
 */
public sealed interface Effect
        permits PassiveEffect, DestroyHero, DestroyAllHeroes, DestroyParty, DestroyRandomHeroes,
                SacrificeHero, EveryoneDiscards, DrawCards, DrawFromDiscard {

    /**
     * Wie oft der Effekt nacheinander ausgeführt wird (z. B. „Ziehe 2 Karten“).
     *
     * @return Anzahl der Wiederholungen, mindestens 1
     */
    default int getCount() {
        return 1;
    }

    /**
     * Liefert eine kurze deutschsprachige Beschreibung für die Oberfläche.
     *
     * @return Beschreibung, z. B. „Ziehe 2 Karten.“
     */
    String describe();
}
