package pp.winf2slay.view.screen;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import pp.winf2slay.view.AppInfo;
import pp.winf2slay.view.ui.ButtonVariant;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.Ui;
import pp.winf2slay.view.ui.UiAnimations;

/**
 * Hauptmenü: Logo links, Menü rechts – darunter dreht sich der Spieltisch.
 */
public class MainMenuScreen extends Screen {

    private Panel logo;
    private float time;
    private Vector3f logoBase;

    /**
     * Erzeugt das Hauptmenü.
     */
    public MainMenuScreen() {
        super(false);
    }

    @Override
    protected void build(Node root, float width, float height) {
        Ui ui = ui();
        float logoSize = Math.min(560, height * 0.55f);
        logo = ui.image("images/logo.png", logoSize, logoSize * 0.9967f);
        logoBase = new Vector3f(width * 0.3f - logoSize / 2, height / 2 + logoSize / 2 + 30, 0);
        logo.setLocalTranslation(logoBase);
        root.attachChild(logo);

        Label tagline = ui.outlined("Das Kartenspiel rund ums WINF-Studium", 30, Theme.CREAM);
        tagline.setTextHAlignment(HAlignment.Center);
        Vector3f ts = tagline.getPreferredSize();
        tagline.setLocalTranslation(width * 0.3f - ts.x / 2, height / 2 - logoSize / 2 + 10, 0);
        root.attachChild(tagline);

        Container menu = ui.panel();
        menu.addChild(ui.heading("Hauptmenü", 44, Theme.GOLD));
        menu.addChild(ui.divider(420));
        menu.addChild(ui.spacer(10, 8));
        addButton(menu, ui.button("Solo spielen", "play_arrow", ButtonVariant.PRIMARY, () -> app().showSolo()));
        addButton(menu, ui.button("Spiel erstellen", "wifi_tethering", ButtonVariant.SECONDARY, () -> app().showHost()));
        addButton(menu, ui.button("Spiel beitreten", "lan", ButtonVariant.SECONDARY, () -> app().showJoin()));
        addButton(menu, ui.button("Einstellungen", "tune", ButtonVariant.SECONDARY, () -> app().showSettings(false)));
        addButton(menu, ui.button("Spielregeln", "menu_book", ButtonVariant.SECONDARY, () -> app().showHelp(false)));
        addButton(menu, ui.button("Beenden", "exit_to_app", ButtonVariant.SECONDARY, () -> app().stop()));
        Vector3f ms = menu.getPreferredSize();
        menu.setLocalTranslation(width * 0.7f - ms.x / 2, height / 2 + ms.y / 2, 0);
        root.attachChild(menu);

        Label footer = ui.text("WinfToSlay " + AppInfo.VERSION + "  ·  Programmierpraktikum  ·  Schriften: Lilita One, Nunito (OFL)  ·  "
                               + "Symbole: Material Icons (Apache 2.0)", 18, Theme.MUTED);
        footer.setLocalTranslation(24, 34, 0);
        root.attachChild(footer);

        UiAnimations.slideIn(app(), menu, 120, 0, 0.05f);
        UiAnimations.slideIn(app(), logo, -120, 0, 0f);
    }

    private void addButton(Container menu, Button button) {
        button.setPreferredSize(new Vector3f(420, 72, 0));
        menu.addChild(button);
        menu.addChild(ui().spacer(4, 10));
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        time += tpf;
        if (logo != null && logoBase != null && time > 0.6f) {
            float bob = FastMath.sin(time * 1.3f) * 8;
            logo.setLocalTranslation(logoBase.x, logoBase.y + bob, logoBase.z);
        }
    }
}
