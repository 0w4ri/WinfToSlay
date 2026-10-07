package pp.winf2slay.view.screen;

import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.TextField;
import pp.winf2slay.controller.message.BotLevel;
import pp.winf2slay.controller.server.state.LobbyState;
import pp.winf2slay.view.settings.UserSettings;
import pp.winf2slay.view.ui.ButtonVariant;
import pp.winf2slay.view.ui.OptionSelector;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.Ui;
import pp.winf2slay.view.ui.UiAnimations;

import java.util.Arrays;
import java.util.List;

/**
 * Einstellungen für ein Solospiel gegen Bots.
 */
public class SoloScreen extends Screen {

    /** Erlaubte Zeichen in Spielernamen. */
    static final java.util.function.Predicate<Character> NAME_CHARS = LobbyState::isAllowedNameChar;

    private TextField name;
    private String nameText;

    /**
     * Erzeugt den Bildschirm.
     */
    public SoloScreen() {
        super(false);
    }

    @Override
    protected void build(Node root, float width, float height) {
        Ui ui = ui();
        UserSettings settings = app().getSettings();
        Container panel = ui.panel();
        panel.addChild(ui.heading("Solospiel", 52, Theme.GOLD));
        panel.addChild(ui.text("Spiele gegen computergesteuerte Gegner – ganz ohne Netzwerk.", 24, Theme.MUTED));
        panel.addChild(ui.divider(720));
        panel.addChild(ui.spacer(10, 12));

        name = ui.textField(nameText != null ? nameText : settings.getPlayerName(), 14, NAME_CHARS, 380);
        panel.addChild(ui.formRow("Dein Name", name));
        panel.addChild(ui.spacer(10, 10));
        panel.addChild(ui.formRow("Gegner", new OptionSelector<>(ui, List.of(1, 2, 3, 4, 5), settings.getSoloBots(),
                                                                 n -> n == 1 ? "1 Bot" : n + " Bots", 260,
                                                                 settings::setSoloBots)));
        List<BotLevel> levels = Arrays.asList(BotLevel.EASY, BotLevel.NORMAL, BotLevel.HARD, null);
        panel.addChild(ui.formRow("Schwierigkeit", new OptionSelector<>(ui, levels, settings.getSoloLevel(),
                                                                        l -> l == null ? "Gemischt" : l.getDisplayName(),
                                                                        260, settings::setSoloLevel)));
        List<Float> speeds = List.of(1.6f, 1f, 0.55f, 0.25f);
        float current = speeds.stream().min((a, b) -> Float.compare(Math.abs(a - settings.getBotSpeed()),
                                                                    Math.abs(b - settings.getBotSpeed()))).orElse(1f);
        panel.addChild(ui.formRow("Bot-Tempo", new OptionSelector<>(ui, speeds, current, SoloScreen::speedName, 260,
                                                                    settings::setBotSpeed)));
        panel.addChild(ui.spacer(10, 20));
        Container buttons = ui.row();
        buttons.addChild(ui.button("Zurück", "arrow_back", ButtonVariant.SECONDARY, this::onBack));
        buttons.addChild(ui.spacer(260, 10));
        buttons.addChild(ui.button("Los geht's!", "play_arrow", ButtonVariant.SUCCESS, this::start));
        panel.addChild(buttons);
        center(panel, width, height, 0);
        root.attachChild(panel);
        UiAnimations.popIn(app(), panel, panel.getPreferredSize());
        ui.focus(name);
    }

    /**
     * @param factor Bedenkzeit-Faktor
     * @return Anzeigename
     */
    static String speedName(float factor) {
        if (factor > 1.2f) return "Gemütlich";
        if (factor > 0.8f) return "Normal";
        if (factor > 0.4f) return "Flott";
        return "Blitzschnell";
    }

    @Override
    protected void beforeResize() {
        nameText = name.getText();
    }

    private void start() {
        String player = name.getText().trim();
        if (player.isEmpty()) player = "Spieler";
        app().getSettings().setPlayerName(player);
        app().startSolo(player);
    }

    @Override
    public void onBack() {
        app().showMainMenu();
    }
}
