package pp.winf2slay.view.screen;

import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import pp.winf2slay.view.settings.UserSettings;
import pp.winf2slay.view.settings.UserSettings.AnimationSpeed;
import pp.winf2slay.view.settings.UserSettings.Quality;
import pp.winf2slay.view.ui.ButtonVariant;
import pp.winf2slay.view.ui.OptionSelector;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.Toggle;
import pp.winf2slay.view.ui.Ui;
import pp.winf2slay.view.ui.UiAnimations;
import pp.winf2slay.view.ui.ValueSlider;

import java.util.List;

/**
 * Einstellungen: Lautstärke, Animationstempo, Bots, Grafik.
 *
 * <p>Alle Änderungen gelten sofort und werden gespeichert.</p>
 */
public class SettingsScreen extends Screen {

    private final Runnable onClose;

    /**
     * @param overlay {@code true}, wenn die Einstellungen über dem Spiel liegen
     * @param onClose wird beim Schließen aufgerufen
     */
    public SettingsScreen(boolean overlay, Runnable onClose) {
        super(overlay);
        this.onClose = onClose;
    }

    @Override
    protected void build(Node root, float width, float height) {
        Ui ui = ui();
        UserSettings s = app().getSettings();
        Container panel = ui.panel();
        panel.addChild(ui.heading("Einstellungen", 52, Theme.GOLD));
        panel.addChild(ui.divider(760));
        panel.addChild(ui.spacer(10, 8));

        panel.addChild(ui.heading("Klang", 32, Theme.GOLD_LIGHT));
        panel.addChild(ui.formRow("Musik", new ValueSlider(ui.theme(), 420, s.getMusicVolume(), s::setMusicVolume)));
        panel.addChild(ui.formRow("Effekte", new ValueSlider(ui.theme(), 420, s.getEffectsVolume(),
                                                             s::setEffectsVolume)));
        panel.addChild(ui.spacer(10, 10));

        panel.addChild(ui.heading("Spiel", 32, Theme.GOLD_LIGHT));
        panel.addChild(ui.formRow("Animationen", new OptionSelector<>(ui, List.of(AnimationSpeed.values()),
                                                                      s.getAnimationSpeed(),
                                                                      AnimationSpeed::getDisplayName, 260,
                                                                      s::setAnimationSpeed)));
        List<Float> speeds = List.of(1.6f, 1f, 0.55f, 0.25f);
        float current = speeds.stream().min((a, b) -> Float.compare(Math.abs(a - s.getBotSpeed()),
                                                                    Math.abs(b - s.getBotSpeed()))).orElse(1f);
        panel.addChild(ui.formRow("Bot-Tempo", new OptionSelector<>(ui, speeds, current, SoloScreen::speedName, 260,
                                                                    s::setBotSpeed)));
        panel.addChild(new Toggle(ui, "Tipps im Spiel anzeigen", s.isShowTips(), s::setShowTips));
        panel.addChild(ui.spacer(10, 10));

        panel.addChild(ui.heading("Grafik", 32, Theme.GOLD_LIGHT));
        panel.addChild(ui.formRow("Qualität", new OptionSelector<>(ui, List.of(Quality.values()), s.getQuality(),
                                                                   Quality::getDisplayName, 260, s::setQuality)));
        panel.addChild(new Toggle(ui, "Kamera wackelt bei Explosionen", s.isCameraShake(), s::setCameraShake));
        panel.addChild(new Toggle(ui, "Bildrate anzeigen", s.isShowFps(), s::setShowFps));
        panel.addChild(ui.spacer(10, 16));
        panel.addChild(ui.button("Fertig", "check", ButtonVariant.SUCCESS, this::onBack));
        center(panel, width, height, 0);
        root.attachChild(panel);
        UiAnimations.popIn(app(), panel, panel.getPreferredSize());
    }

    @Override
    public void onBack() {
        onClose.run();
    }
}
