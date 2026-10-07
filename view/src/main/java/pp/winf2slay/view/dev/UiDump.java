package pp.winf2slay.view.dev;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.TextField;
import pp.winf2slay.controller.client.ClientGameState;
import pp.winf2slay.view.WinfToSlayApp;
import pp.winf2slay.view.game.GameScreen;
import pp.winf2slay.view.game.Stage;
import pp.winf2slay.view.game.board.BoardView;
import pp.winf2slay.view.game.card.CardNode;
import pp.winf2slay.view.game.card.CardSprite;
import pp.winf2slay.view.ui.Dialog;
import pp.winf2slay.view.ui.Toggle;
import pp.winf2slay.view.ui.ValueSlider;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Entwicklerwerkzeug ({@code -Dwinf.uidump=<Datei>}): schreibt mehrmals pro Sekunde
 * alle sichtbaren Bedienelemente und Karten mit ihren Bildschirmkoordinaten als JSON.
 * Damit kann ein Testtreiber Schaltflächen und Karten über ihre Beschriftung bzw.
 * ihren Namen anklicken, statt Koordinaten zu raten.
 */
public class UiDump extends BaseAppState {

    private static final Logger LOGGER = System.getLogger(UiDump.class.getName());

    private final Path file;
    private float timer;
    private float fpsTime;
    private int fpsFrames;
    private int fps;
    private String last = "";

    /**
     * @param file Zieldatei
     */
    public UiDump(Path file) {
        this.file = file;
    }

    @Override
    protected void initialize(Application app) {
        // nichts zu tun
    }

    private static long frame;

    /**
     * @return protokolliert alle Maustasten-Ereignisse; muss vor Lemur angemeldet werden,
     * weil verbrauchte Ereignisse spätere Listener nicht mehr erreichen (Fehlersuche)
     */
    public static com.jme3.input.RawInputListener inputLogger() {
        return new com.jme3.input.RawInputListener() {
            @Override public void beginInput() { }
            @Override public void endInput() { }
            @Override public void onJoyAxisEvent(com.jme3.input.event.JoyAxisEvent evt) { }
            @Override public void onJoyButtonEvent(com.jme3.input.event.JoyButtonEvent evt) { }
            @Override public void onMouseMotionEvent(com.jme3.input.event.MouseMotionEvent evt) { }
            @Override public void onMouseButtonEvent(com.jme3.input.event.MouseButtonEvent evt) {
                LOGGER.log(Level.WARNING, "INPUT frame " + frame + ": button " + evt.getButtonIndex()
                                          + (evt.isPressed() ? " down" : " up") + " at " + evt.getX() + "," + evt.getY());
            }
            @Override public void onKeyEvent(com.jme3.input.event.KeyInputEvent evt) { }
            @Override public void onTouchEvent(com.jme3.input.event.TouchEvent evt) { }
        };
    }

    @Override
    protected void cleanup(Application app) {
        // nichts zu tun
    }

    @Override
    protected void onEnable() {
        // nichts zu tun
    }

    @Override
    protected void onDisable() {
        // nichts zu tun
    }

    @Override
    public void update(float tpf) {
        frame++;
        fpsTime += tpf;
        fpsFrames++;
        if (fpsTime >= 1f) {
            fps = Math.round(fpsFrames / fpsTime);
            fpsTime = 0;
            fpsFrames = 0;
        }
        timer += tpf;
        if (timer < 0.2f) return;
        timer = 0;
        String json = dump();
        if (json.equals(last)) return;
        last = json;
        try {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (IOException e) {
            LOGGER.log(Level.WARNING, "UI-Dump nicht schreibbar: {0}", e.getMessage());
        }
    }

    private String dump() {
        WinfToSlayApp app = (WinfToSlayApp) getApplication();
        Camera cam = app.getCamera();
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"fps\":").append(fps).append(",\"frame\":").append(frame).append(',');
        sb.append("\"w\":").append(cam.getWidth()).append(",\"h\":").append(cam.getHeight());
        sb.append(",\"screen\":").append(str(app.getScreen() == null ? null : app.getScreen().getClass().getSimpleName()));
        sb.append(",\"overlay\":").append(str(app.getOverlay() == null ? null
                                                                      : app.getOverlay().getClass().getSimpleName()));
        sb.append(",\"dialogs\":[");
        appendStrings(sb, Dialog.openTitles());
        sb.append("]");
        GameScreen game = app.getGame();
        Stage stage = game == null ? null : game.getStage();
        if (stage != null) {
            ClientGameState model = stage.model();
            sb.append(",\"game\":{\"me\":").append(str(model.getMyName()))
              .append(",\"active\":").append(str(model.getActivePlayer()))
              .append(",\"myTurn\":").append(model.isMyTurn())
              .append(",\"ap\":").append(model.getActionPoints())
              .append(",\"state\":").append(str(stage.logic().getState().getClass().getSimpleName()))
              .append(",\"prompt\":").append(str(stage.prompts().getTitle()))
              .append(",\"busy\":").append(stage.animator().isBusy())
              .append(",\"hand\":").append(model.getHand().size())
              .append(",\"inspect\":").append(game.isInspecting())
              .append("}");
        }
        sb.append(",\"items\":[");
        List<String> items = new ArrayList<>();
        collectGui(app.getGuiNode(), cam, items, "");
        if (stage != null) {
            collectBoard(app.getTable().getBoard(), cam, stage.board(), items);
            deck(items, cam, "support", stage.board().supportDeck());
            deck(items, cam, "discard", stage.board().discard());
            deck(items, cam, "monster", stage.board().monsterDeck());
        }
        sb.append(String.join(",", items));
        sb.append("]}");
        return sb.toString();
    }

    private static void collectGui(Node node, Camera cam, List<String> out, String field) {
        if (node.getCullHint() == Spatial.CullHint.Always) return;
        for (Spatial child : node.getChildren()) {
            if (child.getCullHint() == Spatial.CullHint.Always) continue;
            String name = child.getName() == null ? "" : child.getName();
            String f = name.startsWith("field:") ? name.substring(6) : field;
            if (child instanceof CardSprite sprite && sprite.getCard() != null) {
                Vector3f p = sprite.getWorldTranslation();
                float w = sprite.getWidth() * sprite.getWorldScale().x;
                out.add(item("hand", sprite.getCard().getName(), p.x, cam.getHeight() - p.y, w, w * CardSprite.RATIO,
                             true, f, p.z, sprite.isGlowing() ? "glow" : null));
            }
            else if (child instanceof Panel panel) {
                String kind = null;
                String text = null;
                boolean enabled = true;
                if (child instanceof ValueSlider v) {
                    kind = "slider";
                    text = String.valueOf(Math.round(v.getValue() * 100));
                }
                else if (child instanceof Toggle t) {
                    kind = "toggle";
                    text = t.getText() + (t.isChecked() ? " [x]" : " [ ]");
                }
                else if (child instanceof Button b) {
                    kind = "button";
                    text = name.startsWith("button:") ? name.substring(7) : b.getText();
                    enabled = b.isEnabled();
                }
                else if (child instanceof TextField t) {
                    kind = "textfield";
                    text = t.getText();
                }
                else if (child instanceof Label l && !l.getText().isEmpty()) {
                    kind = "label";
                    text = l.getText();
                }
                if (kind != null) {
                    Vector3f p = panel.getWorldTranslation();
                    Vector3f size = panel.getSize().clone();
                    Vector3f scale = panel.getWorldScale();
                    float w = size.x * scale.x;
                    float h = size.y * scale.y;
                    out.add(item(kind, text, p.x + w / 2, cam.getHeight() - (p.y - h / 2), w, h, enabled, f, p.z,
                                 null));
                }
            }
            if (child instanceof Node n) collectGui(n, cam, out, f);
        }
    }

    private static void collectBoard(Node node, Camera cam, BoardView board, List<String> out) {
        for (Spatial child : node.getChildren()) {
            if (child.getCullHint() == Spatial.CullHint.Always) continue;
            if (child instanceof CardNode card) {
                BoundingVolume bound = card.getWorldBound();
                Vector3f center = bound == null ? card.getWorldTranslation() : bound.getCenter();
                Vector3f s = cam.getScreenCoordinates(center);
                if (s.z < 0 || s.z > 1) continue;
                BoardView.Place place = board.placeOf(card);
                String where = place == null ? "?" : place.kind() + ":" + place.seat() + ":" + place.index();
                String name = card.getCard() == null ? "(verdeckt)" : card.getCard().getName();
                out.add(item("card", name, s.x, cam.getHeight() - s.y, 0, 0, true, where, s.z,
                             card.getHighlight() == null ? null : card.getHighlight().toString()));
            }
            else if (child instanceof Node n) {
                collectBoard(n, cam, board, out);
            }
        }
    }

    private static void deck(List<String> out, Camera cam, String name, pp.winf2slay.view.game.board.DeckView deck) {
        Vector3f s = cam.getScreenCoordinates(deck.topPosition());
        out.add(item("deck", name, s.x, cam.getHeight() - s.y, 0, 0, true, String.valueOf(deck.getCount()), s.z,
                     null));
    }

    private static String item(String kind, String text, float x, float y, float w, float h, boolean enabled,
                               String field, float z, String extra) {
        return "{\"k\":" + str(kind) + ",\"t\":" + str(text) + ",\"x\":" + Math.round(x) + ",\"y\":" + Math.round(y)
               + ",\"w\":" + Math.round(w) + ",\"h\":" + Math.round(h) + ",\"on\":" + enabled
               + ",\"f\":" + str(field) + ",\"z\":" + z + ",\"e\":" + str(extra) + "}";
    }

    private static void appendStrings(StringBuilder sb, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(str(values.get(i)));
        }
    }

    private static String str(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                default -> {
                    if (c < 32) sb.append(' ');
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }
}
