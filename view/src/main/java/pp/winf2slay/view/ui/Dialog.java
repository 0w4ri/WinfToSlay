package pp.winf2slay.view.ui;

import com.jme3.app.Application;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.event.PopupState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Modaler Dialog mit Überschrift, Text und Schaltflächen.
 *
 * <pre>{@code
 * new Dialog(ui, app, "Verbindung verloren", reason)
 *     .addButton("Zum Menü", ButtonVariant.PRIMARY, app::showMainMenu)
 *     .show();
 * }</pre>
 */
public class Dialog {

    /**
     * Alle offenen Dialoge, der oberste zuletzt.
     */
    private static final Deque<Dialog> OPEN = new ArrayDeque<>();

    private final Ui ui;
    private final Application app;
    private final Node root = new Node("dialog");
    private final Container panel;
    private final Container buttons;
    private final List<Runnable> onClose = new ArrayList<>();
    private boolean open;
    private boolean dismissable = true;
    private final String title;

    /**
     * @param ui    Oberflächenfabrik
     * @param app   Anwendung
     * @param title Überschrift
     * @param text  Text oder {@code null}
     */
    public Dialog(Ui ui, Application app, String title, String text) {
        this.ui = ui;
        this.app = app;
        this.title = title;
        panel = ui.panel();
        Label heading = ui.heading(title);
        heading.setTextHAlignment(HAlignment.Center);
        panel.addChild(heading);
        panel.addChild(ui.divider(520));
        if (text != null) {
            Label body = ui.wrapped(text, Theme.SIZE_TEXT, Theme.CREAM, 640);
            body.setTextHAlignment(HAlignment.Center);
            panel.addChild(body);
            panel.addChild(ui.spacer(10, 14));
        }
        buttons = ui.row();
        panel.addChild(buttons);
        root.attachChild(panel);
    }

    /**
     * Fügt beliebigen Inhalt oberhalb der Schaltflächen ein.
     *
     * @param content Inhalt
     * @return dieser Dialog
     */
    public Dialog addContent(Panel content) {
        panel.removeChild(buttons);
        panel.addChild(content);
        panel.addChild(buttons);
        return this;
    }

    /**
     * @param text    Beschriftung
     * @param variant Variante
     * @param action  Aktion (der Dialog schließt sich vorher)
     * @return dieser Dialog
     */
    public Dialog addButton(String text, ButtonVariant variant, Runnable action) {
        Button b = ui.button(text, variant, () -> {
            close();
            if (action != null) action.run();
        });
        if (!buttons.getLayout().getChildren().isEmpty()) buttons.addChild(ui.spacer(16, 10));
        buttons.addChild(b);
        return this;
    }

    /**
     * @param value {@code false}: Esc schließt den Dialog nicht (z. B. Spielende)
     * @return dieser Dialog
     */
    public Dialog dismissable(boolean value) {
        dismissable = value;
        return this;
    }

    /**
     * @param action wird beim Schließen ausgeführt
     * @return dieser Dialog
     */
    public Dialog onClose(Runnable action) {
        onClose.add(action);
        return this;
    }

    /**
     * Zeigt den Dialog zentriert und modal an.
     */
    public void show() {
        if (open) return;
        open = true;
        OPEN.addLast(this);
        centerButtons();
        float scale = UiScale.of(app.getCamera());
        root.setLocalScale(scale);
        Vector3f size = panel.getPreferredSize();
        float x = (app.getCamera().getWidth() - size.x * scale) / 2;
        float y = (app.getCamera().getHeight() + size.y * scale) / 2;
        root.setLocalTranslation(x, y, 0);
        popups().showModalPopup(root, Theme.OVERLAY);
        UiAnimations.popIn(app, panel, size);
    }

    /**
     * Schiebt die Schaltflächenzeile in die Mitte, wenn der Inhalt breiter ist.
     */
    private void centerButtons() {
        float content = 0;
        for (Node child : panel.getLayout().getChildren()) {
            if (child != buttons && child instanceof Panel p) content = Math.max(content, p.getPreferredSize().x);
        }
        float row = buttons.getPreferredSize().x;
        if (content - row < 2) return;
        List<Node> children = new ArrayList<>(buttons.getLayout().getChildren());
        buttons.clearChildren();
        buttons.addChild(ui.spacer((content - row) / 2, 4));
        children.forEach(buttons::addChild);
    }

    /**
     * Schließt den Dialog.
     */
    public void close() {
        if (!open) return;
        open = false;
        OPEN.remove(this);
        if (popups().isPopup(root)) popups().closePopup(root);
        onClose.forEach(Runnable::run);
    }

    /**
     * @return {@code true}, solange der Dialog sichtbar ist
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * @return {@code true}, solange irgendein Dialog offen ist
     */
    public static boolean anyOpen() {
        return !OPEN.isEmpty();
    }

    /**
     * Esc: schließt den obersten Dialog, sofern erlaubt.
     *
     * @return {@code true}, wenn ein Dialog offen war (Esc ist damit verbraucht)
     */
    public static boolean dismissTop() {
        Dialog top = OPEN.peekLast();
        if (top == null) return false;
        if (top.dismissable) top.close();
        return true;
    }

    /**
     * @return Überschriften aller offenen Dialoge (für Entwicklerwerkzeuge)
     */
    public static List<String> openTitles() {
        List<String> titles = new ArrayList<>();
        for (Dialog d : OPEN) titles.add(d.title);
        return titles;
    }

    /**
     * Schließt alle Dialoge (z. B. beim Verlassen eines Spiels).
     */
    public static void closeAll() {
        for (Dialog d : new ArrayList<>(OPEN)) d.close();
    }

    private PopupState popups() {
        return TopPopupState.of(app);
    }
}
