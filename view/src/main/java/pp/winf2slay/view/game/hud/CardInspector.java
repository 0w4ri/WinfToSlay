package pp.winf2slay.view.game.hud;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.event.PopupState;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.view.game.Texts;
import pp.winf2slay.view.game.card.CardFactory;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.TopPopupState;
import pp.winf2slay.view.ui.Ui;
import pp.winf2slay.view.ui.UiAnimations;
import pp.winf2slay.view.ui.UiScale;

/**
 * Großansicht einer Karte (Rechtsklick). Ein Klick irgendwohin schließt sie.
 */
public class CardInspector {

    private final Ui ui;
    private final com.jme3.app.Application app;
    private final CardFactory cards;
    private Node root;

    /**
     * @param ui    Oberflächenfabrik
     * @param app   Anwendung
     * @param cards Kartenfabrik
     */
    public CardInspector(Ui ui, com.jme3.app.Application app, CardFactory cards) {
        this.ui = ui;
        this.app = app;
        this.cards = cards;
    }

    /**
     * Zeigt eine Karte groß an.
     *
     * @param card Karte
     */
    public void show(Card card) {
        close();
        Camera cam = app.getCamera();
        Container panel = ui.panel();
        Container row = ui.row();
        Label image = new Label("", Theme.STYLE);
        IconComponent icon = new IconComponent(cards.front(card), new Vector2f(1, 1), 0, 0, 0.02f, false);
        icon.setIconSize(new Vector2f(440, 616));
        image.setIcon(icon);
        row.addChild(image);
        row.addChild(ui.spacer(30, 10));
        Container info = ui.column();
        info.addChild(ui.heading(card.getDisplayName(), 54, Theme.GOLD));
        info.addChild(ui.text(Texts.typeLine(card), 26, Theme.MUTED));
        info.addChild(ui.divider(460));
        info.addChild(ui.wrapped(Texts.describe(card), 26, Theme.CREAM, 470));
        info.addChild(ui.spacer(10, 30));
        info.addChild(ui.text("Klicke irgendwo, um die Ansicht zu schließen.", 20, Theme.MUTED));
        row.addChild(info);
        panel.addChild(row);

        root = new Node("inspector");
        root.attachChild(panel);
        float s = UiScale.of(cam);
        root.setLocalScale(s);
        Vector3f size = panel.getPreferredSize();
        root.setLocalTranslation((cam.getWidth() - size.x * s) / 2, (cam.getHeight() + size.y * s) / 2, 0);
        TopPopupState.of(app).showPopup(root, PopupState.ClickMode.ConsumeAndClose, null,
                                                           Theme.OVERLAY);
        UiAnimations.popIn(app, panel, size);
        // Klicks auf das Panel selbst schließen ebenfalls
        com.simsilica.lemur.event.MouseEventControl.addListenersToSpatial(panel,
                new com.simsilica.lemur.event.DefaultMouseListener() {
                    @Override
                    protected void click(com.jme3.input.event.MouseButtonEvent event, com.jme3.scene.Spatial target,
                                         com.jme3.scene.Spatial capture) {
                        close();
                    }
                });
    }

    /**
     * @return {@code true}, solange die Großansicht offen ist
     */
    public boolean isOpen() {
        return root != null && TopPopupState.of(app).isPopup(root);
    }

    /**
     * Schließt die Großansicht.
     */
    public void close() {
        if (isOpen()) TopPopupState.of(app).closePopup(root);
        root = null;
    }
}
