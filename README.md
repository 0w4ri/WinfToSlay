# WinfToSlay

Ein Kartenspiel rund ums WINF-Studium für 2–6 Spieler – im Netzwerk oder allein gegen
computergesteuerte Gegner. Umgesetzt mit [jMonkeyEngine 3.8](https://jmonkeyengine.org) und
[Lemur](https://github.com/jMonkeyEngine-Contributions/Lemur).

> Dieses Repository ist die überarbeitete Fassung des Gruppenprojekts *W2SBattleShips*.
> Der Battleship-Teil liegt jetzt im eigenen Projekt **BattleShips**. Was sich geändert hat,
> steht in [CHANGELOG.md](CHANGELOG.md).

## Funktionen

* **Solospiel** gegen 1–5 Bots (Leicht, Normal, Schwer oder gemischt) – ein Klick im Hauptmenü.
* **Netzwerkspiel**: Gastgeber eröffnen ein Spiel, Mitspieler treten über IP und Port bei.
  Der Gastgeber kann in der Lobby Bots hinzufügen und entfernen. Verlässt jemand ein
  laufendes Spiel, übernimmt ein Bot seinen Platz.
* **3D-Spieltisch** in einer Taverne mit Kerzenlicht, Schatten und Leuchteffekten.
  Eigene Handkarten liegen als Fächer am unteren Rand.
* **Animationen für jedes Spielereignis**: Karten ziehen und ausspielen, Herausforderungen
  mit Schwertern, 3D-Würfel mit echten Augenzahlen, Modifikationen, Monsterangriffe mit
  Krallenspuren, besiegte Monster als Trophäen, Zugwechsel-Banner, Konfetti beim Sieg.
  Die Ritterfigur aus dem Gruppenprojekt steht als Zugmarker auf einem Sockel in der
  Spielerfarbe und hüpft beim Zugwechsel zum nächsten Platz.
* **Effekte der Karten**: Blitz (Held zerstören), Mörsergranate („Mörser 120mm“),
  Hubschrauberangriff („50 Freedoms / Second“), Fliegerbombe mit Druckwelle („GBU48“),
  magische Kreise bei Heldeneffekten, Funkenspuren beim Ziehen, Geisterkarten vom Ablagestapel.
* **Komfort**: Kartenvorschau beim Überfahren, Großansicht per Rechtsklick, Hinweise, was
  gerade möglich ist, Spielverlauf, Regeln im Spiel (F1), Animationen beschleunigen (Leertaste).
* **Einstellungen** (werden gespeichert): Musik- und Effektlautstärke, Animationstempo,
  Bot-Tempo, Grafikqualität, Kamerawackeln, Tipps, Bildrate.

## Spielregeln in Kürze

* Pro Zug hast du **3 Aktionspunkte**: Karte ziehen, Held/Zauber ausspielen und Heldeneffekt
  auslösen kosten je 1 AP, ein Monsterangriff 2 AP (mindestens 2 Helden nötig), eine neue
  Hand 3 AP.
* **Sieg**: 3 besiegte Monster **oder** 5 verschiedene Klassen (der Anführer zählt mit).
* Gespielte Helden und Zauber können **herausgefordert** werden; nach jedem Wurf dürfen alle
  mit **Modifikationen** (±2, ±4) eingreifen.

Die ausführlichen Regeln stehen im Spiel unter *Spielregeln* und als Seite unter
[docs/help/index.html](docs/help/index.html).

## Starten

Voraussetzung: **JDK 21 oder neuer** (empfohlen: Eclipse Temurin 21 oder 23).

```bash
./gradlew :view:run                          # Spiel starten
./gradlew :view:run -Dwinf.autostart=solo    # direkt ein Solospiel
./gradlew test                               # alle Tests
./gradlew :view:installDist                  # Startskript unter view/build/install/winftoslay/bin
```

In IntelliJ liegen fertige Laufkonfigurationen unter `.run/` („WinfToSlay“,
„WinfToSlay Solo-Demo“, „WinfToSlay Effekt-Vorführung“, „Alle Tests“,
„Release-ZIP (WinfToSlay.exe)“, „Release-Setup (Windows-Installer)“).

Für ein Netzwerkspiel auf einem Rechner startet man das Programm zweimal: einmal
*Spiel erstellen*, einmal *Spiel beitreten* mit `localhost`. Im Netz muss beim Gastgeber
der gewählte TCP-Port (Standard 1234) in der Firewall freigegeben sein; UDP wird nicht
benötigt.

**macOS** (Apple Silicon und Intel) funktioniert genauso wie Windows und Linux:
`./gradlew :view:run` setzt dort automatisch den nötigen JVM-Schalter
`-XstartOnFirstThread`. Fehlt nach dem Klonen das Ausführungsrecht, einmal
`chmod +x gradlew`. Auf Trackpads ohne mittlere Maustaste schwenken die Pfeiltasten die
Kamera; Rechtsklick ist ein Klick mit zwei Fingern.

Neuere JDKs (ab 24) melden beim Start Warnungen zu `sun.misc.Unsafe` bzw. nativen
Bibliotheken von LWJGL. Sie sind harmlos; `./gradlew :view:run` blendet sie über passende
JVM-Schalter aus.

## Release bauen (Windows-.exe und macOS-.app)

Das Release ist ein eigenständiges Programm mit eingebauter Java-Laufzeit – Mitspieler
brauchen kein Java. Gebaut wird mit `jpackage` aus dem JDK, und zwar immer für das
Betriebssystem und den Prozessor, auf dem man baut: die `.exe` unter Windows, die `.app`
auf einem Mac (Apple Silicon → `arm64`, Intel → `x64`).

**Alles auf einmal ohne eigenen Mac:** Der Workflow `.github/workflows/release.yml` baut
bei GitHub die ZIPs für Windows, macOS Apple Silicon und macOS Intel und prüft vorher alle
Tests. Auslösen mit einem Tag (`git tag v2.0.1` und `git push origin v2.0.1`) – dann liegt
unter *Releases* ein Entwurf mit allen drei ZIP-Dateien, den man nur noch veröffentlicht.
Alternativ unter *Actions → Release → Run workflow*; die ZIPs hängen dann als Artefakte am
Lauf.

### Windows

```bat
gradlew.bat :view:releaseZip
```

Ergebnis: `build\release\WinfToSlay-<Version>-windows-x64.zip` (≈ 50–60 MB). Darin liegt der
Ordner `WinfToSlay` mit

* `WinfToSlay.exe` – das Spiel,
* `WinfToSlay-Konsole.exe` – dasselbe mit Konsolenfenster, das die Protokollausgabe zeigt
  (für Fehlerberichte),
* `app\` (Spiel) und `runtime\` (Java-Laufzeit).

Die `.exe` funktioniert nur zusammen mit diesen Ordnern: Zum Weitergeben die ZIP-Datei
hochladen (z. B. als GitHub-Release); Mitspieler entpacken sie und starten
`WinfToSlay.exe`. Weil die Datei nicht signiert ist, zeigt Windows beim ersten Start
„Der Computer wurde durch Windows geschützt“ → *Weitere Informationen* → *Trotzdem
ausführen*.

Hinweise:

* Die Version steht nur in `gradle.properties` (`appVersion`); Hauptmenü, Protokoll und
  Dateinamen übernehmen sie. Für ein neues Release dort erhöhen und `CHANGELOG.md` ergänzen.
* Gradle muss mit einem vollständigen JDK ab 21 laufen (in IntelliJ: *Settings → Build,
  Execution, Deployment → Build Tools → Gradle → Gradle JVM*), nicht mit einer reinen
  Laufzeit. Dessen Java-Version wird eingepackt.
* `gradlew.bat :view:appImage` erzeugt nur den Ordner (`view\build\jpackage\image\WinfToSlay`)
  zum schnellen Ausprobieren.
* Optional ein **Installer** (Windows) mit Startmenü-Eintrag und Desktop-Verknüpfung:
  `gradlew.bat :view:installer` → `build\release\WinfToSlay-<Version>.exe`. Dafür muss das
  [WiX Toolset](https://wixtoolset.org/) installiert und im `PATH` sein (WiX 3.14; ab JDK 24
  auch WiX 4 oder 5). Installiert wird ohne Administratorrechte für den angemeldeten Benutzer;
  ein neueres Setup ersetzt eine ältere Installation.

### macOS

Auf einem Mac mit JDK ab 21: `./gradlew :view:releaseZip` →
`build/release/WinfToSlay-<Version>-macos-arm64.zip` (bzw. `-x64` auf Intel-Macs) mit
`WinfToSlay.app`; `./gradlew :view:installer` erzeugt zusätzlich ein `.dmg`. Eine Version
für Apple Silicon läuft nicht auf Intel-Macs und umgekehrt – daher baut der Workflow beide.

Die App ist nicht bei Apple beglaubigt (dafür bräuchte es ein kostenpflichtiges
Entwicklerkonto). Beim ersten Start meldet macOS deshalb, dass die App nicht geprüft werden
konnte. Freigeben: *Systemeinstellungen → Datenschutz & Sicherheit → „Dennoch öffnen“*, oder
im Terminal einmal `xattr -dr com.apple.quarantine /Applications/WinfToSlay.app`.

## Steuerung

| Eingabe | Wirkung |
|---|---|
| Linksklick | Handkarte ausspielen, Held aktivieren, Monster angreifen, Nachziehstapel ziehen |
| Rechtsklick | Karte groß ansehen |
| Mausrad / mittlere Maustaste ziehen oder Pfeiltasten | Zoom / Kamera schwenken |
| Leertaste (halten) | Animationen beschleunigen |
| Z · E · C | Karte ziehen · Zug beenden · Kamera zurücksetzen |
| F1 · Esc | Spielregeln · Menü |

## Projektstruktur

| Modul | Inhalt |
|---|---|
| `model` | Spielregeln ohne Grafik: Karten (`sealed`-Hierarchie), Kartenkatalog, Decks, Effekte, Würfel, `Game` |
| `controller` | Netzwerkprotokoll, Server- und Client-Logik (Zustandsmuster), Bots |
| `view` | jME-Anwendung: Menüs, 3D-Tisch, Hand, Oberfläche, Animationen, Effekte, Sounds |
| `tools/assets` | Python-Skripte, die Schriften, Oberflächen-, Effekt- und Würfeltexturen sowie Sounds erzeugen |
| `docs/help` | Spielregeln als HTML-Seite |

### Architektur

* **Der Server entscheidet.** Alle Regeln werden auf dem Server geprüft und ausgeführt; der
  Client schickt nur Wünsche (`CM…`-Nachrichten).
* **Broadcasts mit Bestätigung.** Jedes Spielereignis (`BC…`) trägt eine `syncId` und den
  vollständigen, für den Empfänger gefilterten Spielzustand. Der Server macht erst weiter,
  wenn alle Clients ihre Animationen beendet und `CMAnimationsDone` geschickt haben
  (mit Zeitüberschreitung). Verdeckte Informationen – etwa gezogene Karten – sehen nur
  die Besitzer.
* **Client-Logik** (`ClientGameLogic`) mit Zuständen *Lobby*, *Warten*, *Mein Zug*,
  *Aktion läuft*, *Spielende* meldet alles als `GameEvent` an die Oberfläche.
* **Oberfläche**: Der `GameDirector` setzt Ereignisse in Animationen um. Der `Animator` spielt
  sie nacheinander ab und ist zugleich das `AnimationGate` der Client-Logik. Danach gleicht
  `BoardView.reconcile` den Tisch mit dem Modell ab – die Anzeige bleibt so auch dann
  korrekt, wenn eine Animation übersprungen wird.
* **Bots** laufen auf dem Server als normale Spieler mit eigener Strategie (`BotStrategy`,
  drei Stärken) und Bedenkzeit.

### Entwicklerschalter

| Eigenschaft | Wirkung |
|---|---|
| `-Dwinf.autostart=solo` | startet sofort ein Solospiel |
| `-Dwinf.autoplay=true` | der eigene Platz spielt automatisch |
| `-Dwinf.showcase=true` | Effekt-Vorführung: ein erfundener Spielverlauf zeigt alle Animationen |
| `-Dwinf.fxtest=true` | spielt alle Spezialeffekte nacheinander in der Tischmitte ab |
| `-Dwinf.record=<Ordner>` | feste Bildrate (20 fps), Bilder werden als JPEG gespeichert |
| `-Dwinf.recordEvery=<n>` | beim Aufzeichnen nur jedes n-te Bild speichern (Standard 4) |
| `-Dwinf.width=…`, `-Dwinf.height=…` | Fenstergröße |
| `-Dwinf.uidump=<Datei>` | schreibt laufend alle Bedienelemente und Karten mit Bildschirmkoordinaten als JSON (für automatisierte Oberflächentests) |
| `-Dwinf.uidump.input=true` | protokolliert zusätzlich Mausklicks und ausgelöste Schaltflächen |
| `-Dwinf.title=…` | Fenstertitel (praktisch, wenn zwei Instanzen nebeneinander laufen) |
| `-Dwinf.testhand=Spell_1,Mage_3,…` | nur für Tests: der erste menschliche Spieler bekommt genau diese Handkarten |
| `-Dwinf.testgroups=<n>` | nur für Tests: jeder Spieler startet mit n Helden in der Gruppe |

## Grafiken, Schriften und Sounds

* Kartenbilder, Logo, Tavernen-Hintergrund, Tischtextur, Ritterfigur und die meisten
  Originalsounds stammen aus dem Gruppenprojekt. Die Karten lagen dort als glTF-Modelle mit
  je eingebetteter Rückseite vor (≈ 270 MB); `tools/assets/migrate_legacy.py` hat daraus
  einheitliche JPEG-Bilder gemacht (≈ 6 MB). Die Kartengeometrie erzeugt das Spiel selbst.
* Schriften: *Lilita One* und *Nunito* (SIL Open Font License 1.1),
  Symbole: *Material Icons* (Apache License 2.0) – Lizenztexte unter
  `view/src/main/resources/licenses`.
* Oberflächen- und Effekttexturen, Würfel, Filzmatte sowie zusätzliche Soundeffekte werden
  von `tools/assets/gen_ui.py`, `gen_fonts.py` und `gen_sounds.py` prozedural erzeugt.
