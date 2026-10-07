package pp.winf2slay.view.screen;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import pp.winf2slay.controller.client.event.DisconnectedEvent;
import pp.winf2slay.controller.client.event.GameEventListener;
import pp.winf2slay.controller.client.event.GameStartedEvent;
import pp.winf2slay.controller.client.event.LobbyChangedEvent;
import pp.winf2slay.controller.client.event.NoticeEvent;
import pp.winf2slay.controller.message.BotLevel;
import pp.winf2slay.controller.message.LobbyEntry;
import pp.winf2slay.model.Game;
import pp.winf2slay.view.net.NetUtil;
import pp.winf2slay.view.net.Session;
import pp.winf2slay.view.ui.ButtonVariant;
import pp.winf2slay.view.ui.OptionSelector;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.Ui;
import pp.winf2slay.view.ui.UiAnimations;

import java.util.ArrayList;
import java.util.List;

/**
 * Lobby vor dem Spielstart. Der Gastgeber kann Bots hinzufügen und entfernen und
 * das Spiel starten. Im Solospiel füllt sich die Lobby automatisch.
 */
public class LobbyScreen extends Screen implements GameEventListener {

    private static final BotLevel[] MIXED = {BotLevel.NORMAL, BotLevel.EASY, BotLevel.HARD};

    private final Session session;
    private List<LobbyEntry> entries = new ArrayList<>();
    private boolean host;
    private BotLevel botLevel = BotLevel.NORMAL;
    private boolean soloBotsRequested;
    private boolean soloStarted;
    private String notice = "";
    private float dots;
    private Label waiting;
    private String pendingDisconnect;
    private GameStartedEvent pendingStart;

    /**
     * @param session Sitzung
     */
    public LobbyScreen(Session session) {
        super(false);
        this.session = session;
        // sofort anmelden: Die ersten Nachrichten können vor onEnable() eintreffen
        session.logic().getEvents().addListener(this);
    }

    @Override
    protected void cleanup(com.jme3.app.Application application) {
        session.logic().getEvents().removeListener(this);
        super.cleanup(application);
    }

    @Override
    protected void onEnable() {
        if (pendingDisconnect != null) {
            String reason = pendingDisconnect;
            pendingDisconnect = null;
            app().enqueue(() -> app().showError("Verbindung getrennt", reason));
        }
        else if (pendingStart != null) {
            GameStartedEvent start = pendingStart;
            pendingStart = null;
            app().enqueue(() -> app().startGame(session, start));
        }
        var model = session.logic().getModel();
        if (entries.isEmpty() && model.getLobby() != null && !model.getLobby().isEmpty()) {
            entries = new ArrayList<>(model.getLobby());
            host = model.isHost();
        }
        updateState();
        super.onEnable();
    }

    @Override
    protected void build(Node root, float width, float height) {
        Ui ui = ui();
        boolean solo = session.mode() == Session.Mode.SOLO;
        Container panel = ui.panel();
        panel.addChild(ui.heading(solo ? "Solospiel" : "Lobby", 52, Theme.GOLD));
        String sub = switch (session.mode()) {
            case SOLO -> "Die Gegner nehmen Platz …";
            case HOST -> {
                List<String> addresses = NetUtil.localAddresses();
                yield "Mitspieler verbinden sich mit " + (addresses.isEmpty() ? "localhost" : addresses.getFirst())
                      + "  ·  Port " + session.port();
            }
            case JOIN -> "Verbunden – " + entries.size() + " von " + Game.MAX_PLAYERS + " Plätzen belegt";
        };
        panel.addChild(ui.text(sub, 24, Theme.MUTED));
        panel.addChild(ui.divider(760));
        panel.addChild(ui.spacer(10, 8));

        Container list = ui.column();
        for (LobbyEntry entry : entries) list.addChild(row(entry));
        // Immer alle Plätze zeigen: Die Tafel behält ihre Größe und die Knöpfe
        // springen beim Hinzufügen von Spielern nicht unter dem Mauszeiger weg.
        for (int i = entries.size(); i < Game.MAX_PLAYERS; i++) list.addChild(freeSlot());
        panel.addChild(list);
        panel.addChild(ui.spacer(10, 10));

        if (host && !solo) {
            Container bots = ui.row();
            Button add = ui.button("Bot hinzufügen", "smart_toy", ButtonVariant.GOLD,
                                   () -> session.logic().addBot(botLevel));
            add.setEnabled(entries.size() < Game.MAX_PLAYERS);
            bots.addChild(add);
            bots.addChild(ui.spacer(14, 10));
            bots.addChild(new OptionSelector<>(ui, List.of(BotLevel.values()), botLevel, BotLevel::getDisplayName, 180,
                                               level -> botLevel = level));
            panel.addChild(bots);
            panel.addChild(ui.spacer(10, 10));
        }
        waiting = ui.text(notice, 24, Theme.CREAM);
        panel.addChild(waiting);
        panel.addChild(ui.spacer(10, 10));

        Container buttons = ui.row();
        buttons.addChild(ui.button(solo ? "Abbrechen" : "Verlassen", "logout", ButtonVariant.SECONDARY, this::onBack));
        if (host && !solo) {
            buttons.addChild(ui.spacer(250, 10));
            Button start = ui.button("Spiel starten", "play_arrow", ButtonVariant.SUCCESS,
                                     () -> session.logic().startGame());
            start.setEnabled(entries.size() >= 2);
            buttons.addChild(start);
        }
        panel.addChild(buttons);
        center(panel, width, height, 0);
        root.attachChild(panel);
        if (entries.isEmpty()) UiAnimations.popIn(app(), panel, panel.getPreferredSize());
    }

    private Container row(LobbyEntry entry) {
        Ui ui = ui();
        Container row = ui.row();
        row.setBackground(ui.rowBackground(entry.getName().equals(session.logic().getModel().getMyName())));
        String icon = entry.isBot() ? "smart_toy" : entry.isHost() ? "star" : "person";
        row.addChild(ui.icon(icon, 34, entry.isHost() ? Theme.GOLD : Theme.CREAM));
        row.addChild(ui.spacer(10, 4));
        Label name = ui.outlined(entry.getName(), 28, Theme.CREAM);
        name.setPreferredSize(new Vector3f(360, 40, 0));
        name.setTextVAlignment(VAlignment.Center);
        row.addChild(name);
        String role = entry.isBot() ? "Bot · " + entry.getLevel().getDisplayName() : entry.isHost() ? "Gastgeber" : "Spieler";
        Label roleLabel = ui.text(role, 22, Theme.MUTED);
        roleLabel.setPreferredSize(new Vector3f(220, 40, 0));
        roleLabel.setTextVAlignment(VAlignment.Center);
        row.addChild(roleLabel);
        if (host && entry.isBot() && session.mode() != Session.Mode.SOLO) {
            row.addChild(ui.roundButton("close", 44, () -> session.logic().removeBot(entry.getName())));
        }
        else {
            row.addChild(ui.spacer(44, 44));
        }
        return row;
    }

    /**
     * Leerer Platz in derselben Höhe wie eine belegte Zeile.
     */
    private Container freeSlot() {
        Ui ui = ui();
        ColorRGBA faint = Theme.withAlpha(Theme.MUTED, 0.45f);
        Container row = ui.row();
        QuadBackgroundComponent bg = ui.rowBackground(false);
        bg.setColor(new ColorRGBA(0, 0, 0, 0.08f));
        row.setBackground(bg);
        row.addChild(ui.icon("person", 34, faint));
        row.addChild(ui.spacer(10, 4));
        Label label = ui.text("Freier Platz", 24, faint);
        label.setPreferredSize(new Vector3f(580, 40, 0));
        label.setTextVAlignment(VAlignment.Center);
        row.addChild(label);
        row.addChild(ui.spacer(44, 44));
        return row;
    }

    private void requestSoloBots() {
        if (session.mode() == Session.Mode.SOLO && !soloBotsRequested) {
            soloBotsRequested = true;
            int count = app().getSettings().getSoloBots();
            BotLevel level = app().getSettings().getSoloLevel();
            for (int i = 0; i < count; i++) {
                session.logic().addBot(level != null ? level : MIXED[i % MIXED.length]);
            }
        }
    }

    @Override
    public void onLobbyChanged(LobbyChangedEvent event) {
        entries = new ArrayList<>(event.entries());
        host = event.host();
        if (!isInitialized()) return;
        updateState();
        rebuild();
    }

    /**
     * Aktualisiert den Hinweistext; im Solospiel startet die Partie, sobald alle Bots da sind.
     */
    private void updateState() {
        switch (session.mode()) {
            case SOLO -> {
                if (host) requestSoloBots();
                if (!soloStarted && entries.size() >= 1 + app().getSettings().getSoloBots()) {
                    soloStarted = true;
                    session.logic().startGame();
                }
                notice = soloStarted ? "Das Spiel beginnt …" : "Die Bots setzen sich an den Tisch …";
            }
            case HOST -> notice = entries.size() < 2 ? "Füge Bots hinzu oder warte auf Mitspieler."
                                                    : "Bereit – starte, sobald alle am Tisch sitzen.";
            case JOIN -> notice = "Warte auf den Gastgeber";
        }
    }

    @Override
    public void onNotice(NoticeEvent event) {
        if (!isEnabled()) return;
        notice = event.text();
        if (waiting != null) waiting.setText(notice);
    }

    @Override
    public void onDisconnected(DisconnectedEvent event) {
        if (!isInitialized()) {
            // Abweisung kurz nach dem Verbindungsaufbau – im selben Bild wie der Wechsel hierher
            pendingDisconnect = event.reason();
            return;
        }
        app().showError("Verbindung getrennt", event.reason());
    }

    @Override
    public void onGameStarted(GameStartedEvent event) {
        if (!isInitialized()) {
            pendingStart = event;
            return;
        }
        app().startGame(session, event);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        if (session.mode() == Session.Mode.JOIN && waiting != null && notice.startsWith("Warte")) {
            dots += tpf * 2.5f;
            waiting.setText(notice + " " + ".".repeat(1 + ((int) dots) % 3));
        }
    }

    @Override
    public void onBack() {
        app().leaveSession();
        app().showMainMenu();
    }
}
