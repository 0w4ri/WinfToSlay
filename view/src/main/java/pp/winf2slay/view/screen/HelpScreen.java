package pp.winf2slay.view.screen;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import pp.winf2slay.model.Action;
import pp.winf2slay.model.Game;
import pp.winf2slay.view.ui.ButtonVariant;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.Ui;
import pp.winf2slay.view.ui.UiAnimations;

import java.util.List;

/**
 * Spielregeln und Steuerung, nach Themen gegliedert.
 */
public class HelpScreen extends Screen {

    /**
     * Ein Kapitel der Hilfe.
     *
     * @param title    Überschrift
     * @param icon     Symbol
     * @param sections Abschnitte (Überschrift, Text)
     */
    private record Topic(String title, String icon, List<String[]> sections) {}

    private static final List<Topic> TOPICS = List.of(
            new Topic("Überblick", "info", List.of(
                    new String[]{"Worum geht es?",
                                 "Ihr seid Studierende der Wirtschaftsinformatik und stellt eine Heldengruppe zusammen, "
                                 + "um Monster wie Klausuren und Hexidentin zu besiegen. Jeder Held gehört zu einer "
                                 + "Klasse (Magier, Wächter, Kämpfer, Waldläufer, Dieb, Barde)."},
                    new String[]{"Sieg",
                                 "Du gewinnst sofort, wenn du " + Game.MONSTERS_TO_WIN + " Monster besiegt hast "
                                 + "oder " + Game.CLASSES_TO_WIN + " verschiedene Klassen besitzt. Dein Anführer "
                                 + "zählt dabei als eine Klasse."},
                    new String[]{"Spielstart",
                                 "Jeder erhält einen Anführer und " + Game.HAND_SIZE + " Karten. Drei Monster liegen "
                                 + "offen in der Tischmitte."})),
            new Topic("Dein Zug", "bolt", List.of(
                    new String[]{"Aktionspunkte",
                                 "Pro Zug hast du " + Action.POINTS_PER_TURN + " Aktionspunkte (AP). Aktionen sind "
                                 + "frei kombinierbar und dürfen mehrfach ausgeführt werden."},
                    new String[]{"1 AP",
                                 "Karte ziehen · Held oder Zauber ausspielen · Effekt eines Helden auslösen "
                                 + "(jeder Held einmal pro Zug)."},
                    new String[]{Action.ATTACK_MONSTER.getCost() + " AP",
                                 "Ein Monster angreifen – dafür brauchst du mindestens " + Action.HEROES_TO_ATTACK
                                 + " Helden in deiner Gruppe."},
                    new String[]{Action.MULLIGAN.getCost() + " AP",
                                 "Neue Hand: alle Handkarten ablegen und " + Game.HAND_SIZE + " neue ziehen."})),
            new Topic("Karten", "style", List.of(
                    new String[]{"Helden",
                                 "Kommen in deine Gruppe (höchstens 5). Beim Ausspielen und später gegen 1 AP würfelst "
                                 + "du: Erreichst du den Wert auf der Karte, tritt ihr Effekt ein."},
                    new String[]{"Zauber",
                                 "Wirken sofort – vom Frühstück, das Karten bringt, bis zur Fliegerbombe, die alle "
                                 + "Helden zerstört."},
                    new String[]{"Modifikationen",
                                 "Verändern einen Würfelwurf um ±2 oder ±4. Du spielst sie, nachdem gewürfelt wurde – "
                                 + "auch im Zug der anderen."},
                    new String[]{"Herausforderungen",
                                 "Spielt jemand einen Helden oder Zauber, kannst du ihn herausfordern. Beide würfeln; "
                                 + "verliert der Ausspielende, landet die Karte auf dem Ablagestapel."},
                    new String[]{"Anführer & Monster",
                                 "Anführer geben dauerhafte Boni. Besiegte Monster ebenso – misslingt ein Angriff, "
                                 + "musst du einen Helden opfern."})),
            new Topic("Würfeln", "casino", List.of(
                    new String[]{"So wird gewürfelt",
                                 "Es wird immer mit zwei Würfeln gewürfelt. Boni von Anführer und Monstern werden "
                                 + "automatisch addiert."},
                    new String[]{"Ziele",
                                 "Heldeneffekte und die meisten Monster verlangen einen Mindestwert. Hommelgoyle ist "
                                 + "anders: Hier musst du höchstens den Zielwert würfeln."},
                    new String[]{"Eingreifen",
                                 "Nach jedem Wurf werden alle Spieler mit Modifikationskarten gefragt, ob sie das "
                                 + "Ergebnis verändern möchten."})),
            new Topic("Bots", "smart_toy", List.of(
                    new String[]{"Allein spielen",
                                 "Im Hauptmenü startet „Solo spielen“ eine Partie gegen 1 bis 5 Bots. Wähle "
                                 + "Leicht, Normal, Schwer oder Gemischt."},
                    new String[]{"Bots im Netzwerkspiel",
                                 "Der Gastgeber kann in der Lobby Bots hinzufügen und entfernen. Verlässt ein Mensch "
                                 + "das laufende Spiel, übernimmt ein Bot seinen Platz."},
                    new String[]{"Tempo",
                                 "Wie schnell die Bots handeln, stellst du unter Einstellungen » Bot-Tempo ein."})),
            new Topic("Steuerung", "mouse", List.of(
                    new String[]{"Maus",
                                 "Linksklick: Karte ausspielen, Held aktivieren, Monster angreifen, Nachziehstapel "
                                 + "ziehen. Rechtsklick: Karte groß ansehen. Mausrad: Zoom. Mittlere Maustaste ziehen: "
                                 + "Kamera schwenken."},
                    new String[]{"Tastatur",
                                 "Esc: Menü · F1: Spielregeln · Leertaste halten: Animationen beschleunigen · "
                                 + "C: Kamera zurücksetzen · Z: Karte ziehen · E: Zug beenden"},
                    new String[]{"Hervorhebungen",
                                 "Goldenes Leuchten zeigt, was du gerade spielen kannst. Rot leuchtende Helden sind "
                                 + "Ziele, wenn du einen Helden auswählen sollst."})));

    private final Runnable onClose;
    private int selected;

    /**
     * @param overlay {@code true}, wenn die Hilfe über dem Spiel liegt
     * @param onClose wird beim Schließen aufgerufen
     */
    public HelpScreen(boolean overlay, Runnable onClose) {
        super(overlay);
        this.onClose = onClose;
    }

    @Override
    protected void build(Node root, float width, float height) {
        Ui ui = ui();
        Container panel = ui.panel();
        Container head = ui.row();
        head.addChild(ui.icon("menu_book", 52, Theme.GOLD));
        head.addChild(ui.spacer(12, 4));
        head.addChild(ui.heading("Spielregeln", 52, Theme.GOLD));
        panel.addChild(head);
        panel.addChild(ui.divider(1180));
        Container body = ui.row();
        Container tabs = ui.column();
        for (int i = 0; i < TOPICS.size(); i++) {
            int index = i;
            Topic t = TOPICS.get(i);
            Button b = ui.button(t.title(), t.icon(), i == selected ? ButtonVariant.GOLD : ButtonVariant.SECONDARY,
                                 () -> {
                                     selected = index;
                                     rebuild();
                                 });
            b.setPreferredSize(new Vector3f(300, 64, 0));
            tabs.addChild(b);
            tabs.addChild(ui.spacer(4, 8));
        }
        body.addChild(tabs);
        body.addChild(ui.spacer(30, 10));
        // Höhe am längsten Thema ausrichten: Der Text passt immer hinein, und die Tafel
        // springt beim Wechsel der Themen nicht.
        float contentHeight = 560;
        for (Topic t : TOPICS) contentHeight = Math.max(contentHeight, topicContent(ui, t).getPreferredSize().y);
        Container content = topicContent(ui, TOPICS.get(selected));
        content.setPreferredSize(new Vector3f(840, contentHeight, 0));
        body.addChild(content);
        panel.addChild(body);
        panel.addChild(ui.spacer(10, 10));
        panel.addChild(ui.button("Schließen", "close", ButtonVariant.PRIMARY, this::onBack));
        center(panel, width, height, 0);
        root.attachChild(panel);
        UiAnimations.fadeIn(app(), panel, 0.2f);
    }

    private static Container topicContent(Ui ui, Topic topic) {
        Container content = ui.column();
        content.addChild(ui.heading(topic.title(), 42, Theme.GOLD_LIGHT));
        for (String[] section : topic.sections()) {
            content.addChild(ui.spacer(8, 10));
            content.addChild(ui.outlined(section[0], 28, Theme.GOLD));
            Label text = ui.wrapped(section[1], 25, Theme.CREAM, 820);
            content.addChild(text);
        }
        return content;
    }

    @Override
    public void onBack() {
        onClose.run();
    }
}
