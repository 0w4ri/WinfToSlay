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

/**
 * Einem Netzwerkspiel beitreten.
 */
public class JoinScreen extends Screen {

    private TextField name;
    private TextField host;
    private TextField port;
    private Label status;
    private boolean connecting;
    private float dots;
    private String nameText;
    private String hostText;
    private String portText;
    private String statusText = "";

    /**
     * Erzeugt den Bildschirm.
     */
    public JoinScreen() {
        super(false);
    }

    @Override
    protected void build(Node root, float width, float height) {
        Ui ui = ui();
        Container panel = ui.panel();
        panel.addChild(ui.heading("Spiel beitreten", 52, Theme.GOLD));
        panel.addChild(ui.text("Gib die Adresse des Gastgebers ein.", 24, Theme.MUTED));
        panel.addChild(ui.divider(720));
        panel.addChild(ui.spacer(10, 12));
        name = ui.textField(nameText != null ? nameText : app().getSettings().getPlayerName(), 14,
                            SoloScreen.NAME_CHARS, 380);
        panel.addChild(ui.formRow("Dein Name", name));
        panel.addChild(ui.spacer(10, 8));
        host = ui.textField(hostText != null ? hostText : app().getSettings().getLastHost(), 60,
                            c -> c < 128 && (Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == ':'), 380);
        panel.addChild(ui.formRow("Server", host));
        panel.addChild(ui.spacer(10, 8));
        port = ui.textField(portText != null ? portText : String.valueOf(app().getSettings().getLastPort()), 5,
                            Character::isDigit, 200);
        panel.addChild(ui.formRow("Port", port));
        panel.addChild(ui.spacer(10, 10));
        status = ui.text(connecting ? "Verbinde" : statusText, 24, connecting ? Theme.CREAM : Theme.RED);
        panel.addChild(status);
        panel.addChild(ui.spacer(10, 10));
        Container buttons = ui.row();
        buttons.addChild(ui.button("Zurück", "arrow_back", ButtonVariant.SECONDARY, this::onBack));
        buttons.addChild(ui.spacer(250, 10));
        buttons.addChild(ui.button("Verbinden", "lan", ButtonVariant.SUCCESS, this::connect));
        panel.addChild(buttons);
        center(panel, width, height, 0);
        root.attachChild(panel);
        UiAnimations.popIn(app(), panel, panel.getPreferredSize());
        ui.focus(name);
    }

    @Override
    protected void beforeResize() {
        nameText = name.getText();
        hostText = host.getText();
        portText = port.getText();
        statusText = connecting ? "" : status.getText();
    }

    private void connect() {
        if (connecting) return;
        String player = name.getText().trim();
        String server = host.getText().trim();
        int p = NetUtil.parsePort(port.getText());
        if (player.isEmpty() || server.isEmpty()) {
            status.setText("Bitte Name und Server angeben.");
            return;
        }
        if (p < 0) {
            status.setText("Der Port muss eine Zahl zwischen 1 und 65535 sein.");
            return;
        }
        app().getSettings().setPlayerName(player);
        app().getSettings().setLastHost(server);
        app().getSettings().setLastPort(p);
        connecting = true;
        status.setColor(Theme.CREAM.clone());
        status.setText("Verbinde");
        app().startJoin(player, server, p, error -> {
            connecting = false;
            status.setColor(Theme.RED.clone());
            status.setText(error);
            statusText = error;
        });
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        if (connecting && status != null) {
            dots += tpf * 3;
            status.setText("Verbinde" + ".".repeat(1 + ((int) dots) % 3));
        }
    }

    @Override
    public void onBack() {
        app().leaveSession();
        app().showMainMenu();
    }
}
