package pp.winf2slay.view.screen;

import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.TextField;
import pp.winf2slay.view.net.NetUtil;
import pp.winf2slay.view.ui.ButtonVariant;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.Ui;
import pp.winf2slay.view.ui.UiAnimations;

import java.util.List;

/**
 * Ein Netzwerkspiel eröffnen (eigener Server).
 */
public class HostScreen extends Screen {

    private TextField name;
    private TextField port;
    private Label status;
    private String nameText;
    private String portText;
    private String statusText = "";

    /**
     * Erzeugt den Bildschirm.
     */
    public HostScreen() {
        super(false);
    }

    @Override
    protected void build(Node root, float width, float height) {
        Ui ui = ui();
        Container panel = ui.panel();
        panel.addChild(ui.heading("Spiel erstellen", 52, Theme.GOLD));
        panel.addChild(ui.text("Du bist Gastgeber: Mitspieler im selben Netz verbinden sich mit deinem Rechner.", 24,
                               Theme.MUTED));
        panel.addChild(ui.divider(760));
        panel.addChild(ui.spacer(10, 12));
        name = ui.textField(nameText != null ? nameText : app().getSettings().getPlayerName(), 14,
                            SoloScreen.NAME_CHARS, 380);
        panel.addChild(ui.formRow("Dein Name", name));
        panel.addChild(ui.spacer(10, 8));
        port = ui.textField(portText != null ? portText : String.valueOf(app().getSettings().getLastPort()), 5,
                            Character::isDigit, 200);
        panel.addChild(ui.formRow("Port", port));
        panel.addChild(ui.spacer(10, 12));
        List<String> addresses = NetUtil.localAddresses();
        String hint = addresses.isEmpty() ? "Keine Netzwerkadresse gefunden – Mitspieler auf diesem Rechner nutzen "
                                            + "„localhost“."
                                          : "Deine Adresse für Mitspieler: "
                                            + String.join("  oder  ", addresses.subList(0, Math.min(3, addresses.size())));
        panel.addChild(ui.wrapped(hint, 24, Theme.CREAM, 740));
        panel.addChild(ui.wrapped("In der Lobby kannst du zusätzlich Bots hinzufügen.", 22, Theme.MUTED, 740));
        status = ui.text(statusText, 24, Theme.RED);
        panel.addChild(status);
        panel.addChild(ui.spacer(10, 10));
        Container buttons = ui.row();
        buttons.addChild(ui.button("Zurück", "arrow_back", ButtonVariant.SECONDARY, this::onBack));
        buttons.addChild(ui.spacer(280, 10));
        buttons.addChild(ui.button("Lobby öffnen", "wifi_tethering", ButtonVariant.SUCCESS, this::start));
        panel.addChild(buttons);
        center(panel, width, height, 0);
        root.attachChild(panel);
        UiAnimations.popIn(app(), panel, panel.getPreferredSize());
        ui.focus(name);
    }

    @Override
    protected void beforeResize() {
        nameText = name.getText();
        portText = port.getText();
        statusText = status.getText();
    }

    private void start() {
        String player = name.getText().trim();
        int p = NetUtil.parsePort(port.getText());
        if (player.isEmpty()) {
            status.setText("Bitte gib einen Namen ein.");
            return;
        }
        if (p < 0) {
            status.setText("Der Port muss eine Zahl zwischen 1 und 65535 sein.");
            return;
        }
        app().getSettings().setPlayerName(player);
        app().getSettings().setLastPort(p);
        String error = app().startHost(player, p);
        if (error != null) status.setText(error);
    }

    @Override
    public void onBack() {
        app().showMainMenu();
    }
}
