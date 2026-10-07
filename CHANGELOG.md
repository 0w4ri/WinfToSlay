# Änderungen

## 2.0.1 – Korrekturen nach dem ersten Test (7. Oktober 2026)

Nach einem Absturz beim Öffnen von *Solo spielen* wurde jeder Bildschirm, jedes
Bedienelement und jede Spielaktion automatisiert über echte Mausklicks durchgespielt
(Solo, Netzwerk mit zwei Programmen, Spielende, Abbrüche). Dabei gefundene und behobene
Fehler:

* **Absturz beim Solo-Bildschirm** – die Auswahl „Gemischt“ (intern ohne Wert) brachte
  die Auswahlliste zum Absturz.
* **„Nochmal spielen“ und jedes zweite Spiel** schlugen fehl: jME sperrt beim ersten
  Server das Nachrichtenregister; ein zweiter Server im selben Programm scheiterte.
* **Mitspieler erfuhren nicht, wenn der Gastgeber das Spiel beendet** – der Server wurde
  beendet, bevor die Abmeldungen verschickt waren. Jetzt erscheint „Verbindung getrennt“.
* **Fehlermeldungen lagen hinter dem Hauptmenü** und waren nicht zu sehen.
* **Klicks gingen verloren**, wenn Drücken und Loslassen ins selbe Bild fielen (langsame
  Rechner, schnelle Klicks): Schaltflächen tauschen jetzt nur noch die Textur.
* **Belegter Port** beim Erstellen eines Spiels führte zum Absturz statt zur Meldung
  „Port belegt“; Solospiele probieren automatisch einen anderen Port.
* **Unerwartete Fehler** beenden das Programm nicht mehr: Es erscheint ein Hinweis mit der
  Wahl „Weiter“ oder „Zum Hauptmenü“.
* **Netzwerk** robuster: nur noch TCP (kein UDP nötig, keine hängenden Beitritte hinter
  Firewalls), Zeitlimit beim Verbindungsaufbau, verständliche Meldungen bei falscher
  Adresse/Version, abgebrochener Verbindungsaufbau hinterlässt keinen „Geisterspieler“,
  Abweisung („Spiel voll oder läuft bereits“) erscheint zuverlässig auf dem
  Beitreten-Bildschirm (die Lobby öffnet sich erst nach der Begrüßung durch den Server,
  ohne Fehlermeldung im Protokoll des Gastgebers), Spielleitung geht beim Verlassen der
  Lobby an den nächsten Menschen.
* **Menü-Dekoration** auf dem Tisch verschwand nach dem ersten Spiel.
* **Dialoge** blieben nach dem Verlassen eines Spiels offen; Esc öffnete mehrere
  Pausenmenüs übereinander. Esc schließt jetzt den obersten Dialog.
* **Tastenkürzel** (Z, E …) wirkten auch durch Dialoge und Einstellungen hindurch.
* **Minuszeichen und Pfeile** fehlten in der Schrift und waren unsichtbar („−4“ wurde zu
  „4“). Ein Test prüft jetzt alle Texte gegen die Schriften.
* **Eingaben** gingen bei einer Änderung der Fenstergröße verloren.
* **Regeln, Thema „Karten“**: Text lief über den Rand.
* **Kontrollkästchen** reagieren jetzt auf die ganze Zeile, nicht nur auf den Text.
* Spielernamen: Eingabefeld und Server erlauben dieselben Zeichen.
* Der Ablagestapel blieb nach „Karte vom Ablagestapel holen“ manchmal unsichtbar.
* Spielende-Meldung nennt den Grund („… hat 3 Monster besiegt“); das Banner zeigt
  „Du hast gewonnen!“ bzw. „Spielende“.
* Die Adresse für Mitspieler bevorzugt echte Netzwerkkarten vor virtuellen (WSL, Docker …).
* Neuere JDKs: `./gradlew :view:run` blendet die harmlosen LWJGL-Warnungen aus.
* **Release als Programm**: `gradlew :view:releaseZip` baut mit `jpackage` eine ZIP-Datei mit
  `WinfToSlay.exe` und eigener Java-Laufzeit (dazu `WinfToSlay-Konsole.exe` für
  Fehlerberichte); optional `gradlew :view:installer` für ein Windows-Setup (WiX). Die
  Versionsnummer steht nur noch in `gradle.properties`.
* **macOS**: Release als `WinfToSlay.app` (ZIP bzw. `.dmg`, Apple Silicon und Intel); der
  GitHub-Workflow `release.yml` baut Windows- und Mac-Versionen auf einmal. Im Programm:
  kein AWT im Mac-Betrieb (sonst droht ein Hänger mit `-XstartOnFirstThread`), Kamera
  zusätzlich per Pfeiltasten für Trackpads ohne mittlere Maustaste.

Neue Tests: Lobby-Regeln, Spieler, mehrere Server nacheinander, belegter Port,
Benachrichtigung beim Serverende, abgebrochener Verbindungsaufbau, Abweisung bei voller
Lobby, Schriftabdeckung.
Für die automatisierten Oberflächentests gibt es den Entwicklerschalter
`-Dwinf.uidump=<Datei>`.

## 2.0 – Überarbeitung (Oktober 2026)

Ausgangspunkt war das Gruppenprojekt *W2SBattleShips*, in dem WinfToSlay und Battleship
gemeinsam mit den Hilfsmodulen `common` und `jme-common` in einem Gradle-Build lagen.

### Projekt

* **Aufgeteilt**: Battleship liegt mit seinen Hilfsmodulen im eigenen Projekt *BattleShips*.
  WinfToSlay ist ein eigenständiges Gradle-Projekt mit den Modulen `model`, `controller` und
  `view` und braucht keine gemeinsamen Module mehr.
* Einheitlicher Build über `buildSrc`-Konventionen und einen Versionskatalog
  (`gradle/libs.versions.toml`): Java 21, jMonkeyEngine 3.8.1, Lemur 1.16, JUnit 5.
* Paketstruktur nach Zuständigkeiten statt technischer Sammelordner (z. B.
  `controller.message.client/server`, `controller.server.bot`, `view.game.board`,
  `view.game.hud`, `view.anim`, `view.fx`).
* Ressourcen von ≈ 343 MB auf ≈ 19 MB verkleinert: Die Karten lagen als einzelne
  glTF-Modelle vor; sie sind jetzt JPEG-Bilder, die Kartengeometrie erzeugt das Spiel selbst
  (`tools/assets/migrate_legacy.py`).
* IntelliJ-Laufkonfigurationen unter `.run/`, README, diese Datei und eine neue Regelseite
  (`docs/help/index.html`).

### Spiellogik (`model`)

* Karten als `sealed`-Hierarchie (Held, Zauber, Modifikation, Herausforderung, Anführer,
  Monster) mit einem zentralen Kartenkatalog.
* Effekte als eigene Klassen mit klarer Trennung zwischen passiven Boni und ausgelösten
  Effekten; Würfel mit austauschbarem Zufallsgenerator (reproduzierbare Tests).
* Siegbedingung **5 verschiedene Klassen** (Anführer zählt mit) – wie in der ursprünglichen
  Implementierung; die alte Hilfeseite sprach fälschlich von 6. Der Wert steht als
  `Game.CLASSES_TO_WIN` an einer Stelle.

### Netzwerk und Ablauf (`controller`)

* **Der Server entscheidet**: Alle Regeln werden auf dem Server geprüft; Clients schicken nur
  Wünsche. Der Client prüft dieselben Regeln nur, um Schaltflächen sinnvoll zu sperren.
* **Broadcasts mit Bestätigung** (`syncId` + `CMAnimationsDone`, mit Zeitüberschreitung):
  Alle Clients sehen jedes Ereignis vollständig animiert, bevor das Spiel weitergeht.
  Verdeckte Informationen werden pro Empfänger gefiltert.
* Client- und Serverlogik als Zustandsautomaten; die Oberfläche erhält ausschließlich
  `GameEvent`s.
* **Bots** in drei Stärken (Leicht, Normal, Schwer) mit Bedenkzeit. Sie spielen auf dem
  Server wie normale Spieler, können in der Lobby hinzugefügt und entfernt werden und
  übernehmen den Platz von Spielern, die ein laufendes Spiel verlassen.
* Lobby: eindeutige Namen, höchstens 6 Spieler, Start ab 2 Spielern, Spielleitung geht an
  den nächsten Menschen über, wenn der Spielleiter die Lobby verlässt.

### Oberfläche (`view`)

* Komplett neu: Hauptmenü vor einem Tavernen-Panorama, Solo-, Gastgeber-, Beitreten-,
  Lobby-, Einstellungs- und Regelbildschirm in einem einheitlichen Stil (eigene Schriften,
  Symbole, Schaltflächen, Dialoge). Die Oberfläche skaliert mit der Fenstergröße.
* **Solo spielen** mit einem Klick (1–5 Bots, Stärke wählbar).
* 3D-Spieltisch mit Kerzenlicht, Schatten, Bloom und Filzmatte; eigene Handkarten als
  Fächer, Vorschau beim Überfahren, Großansicht per Rechtsklick, Hervorhebung spielbarer
  Karten und gültiger Ziele, Spielverlauf, Hinweise.
* **Animationen für jedes Ereignis**, abgespielt von einem eigenen Animationssystem
  (Tweens, Hauptspur mit Watchdog, Beschleunigen per Leertaste):
  Ziehen mit Funkenspur, Ausspielen, Herausforderungen mit gekreuzten Schwertern, 3D-Würfel,
  Modifikationen, magische Kreise bei Heldeneffekten, Monsterangriffe mit Kamerafahrt und
  Krallenspuren, Trophäen, Zugwechsel-Banner, Konfetti beim Sieg.
* **Kartenspezifische Effekte**: Blitz (Held zerstören), Mörsergranate, Hubschrauberangriff,
  Fliegerbombe mit Druckwelle, magisch zurückgeholte Karten vom Ablagestapel u. a.
* Die **Ritterfigur** aus dem Gruppenprojekt dient als Zugmarker und hüpft zum Spieler am Zug.
* Einstellungen werden gespeichert: Lautstärken, Animations- und Bot-Tempo, Grafikqualität,
  Kamerawackeln, Tipps, Bildrate.
* Fehler in Animationen oder Ereignissen bringen das Spiel nicht mehr zum Absturz; ein
  Verbindungsabbruch führt zu einer verständlichen Meldung.

### Tests

* `model`: Karten, Decks, Felder, Würfel, Effekte, Spieler und Siegbedingungen.
* `controller`: Serialisierung aller Karten und typischer Nachrichten (inkl. verdeckter
  Informationen), Lobby-Regeln, komplette Partien zweier
  Clients mit Bots (virtuelle Zeit, mehrere Seeds) und Bot-Simulationen.
* `view`: Animationssystem (Tweens, Spuren, Zeitweitergabe, Watchdog).
