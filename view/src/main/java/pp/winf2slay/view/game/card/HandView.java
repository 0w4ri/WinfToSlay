package pp.winf2slay.view.game.card;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.MouseEventControl;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.UiScale;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Die eigenen Handkarten als Fächer am unteren Bildschirmrand.
 *
 * <p>Karten bewegen sich weich zu ihren Zielpositionen. Die Karte unter der Maus
 * wird angehoben und vergrößert; spielbare Karten leuchten golden.</p>
 */
public class HandView {

    /**
     * Empfänger von Aktionen auf Handkarten.
     */
    public interface Listener {
        /**
         * @param card angeklickte Karte (Linksklick)
         */
        void play(Card card);

        /**
         * @param card Karte für die Großansicht (Rechtsklick)
         */
        void inspect(Card card);
    }

    /** Kartenbreite in Referenzpixeln. */
    public static final float CARD_WIDTH = 150f;
    private static final float BASE_Y = 105f;
    private static final float HOVER_Y = 225f;
    private static final float HOVER_SCALE = 1.55f;

    private final CardFactory factory;
    private final Camera cam;
    private final Listener listener;
    private final Node root = new Node("hand");
    private final List<CardSprite> sprites = new ArrayList<>();
    private final List<CardSprite> leaving = new ArrayList<>();
    private final MouseHandler mouse = new MouseHandler();
    private Predicate<Card> playable = c -> false;
    private CardSprite hovered;
    private boolean interactive = true;

    /**
     * @param factory  Kartenfabrik
     * @param guiNode  Oberflächenknoten
     * @param cam      Kamera (Fenstergröße)
     * @param listener Empfänger von Klicks
     */
    public HandView(CardFactory factory, Node guiNode, Camera cam, Listener listener) {
        this.factory = factory;
        this.cam = cam;
        this.listener = listener;
        guiNode.attachChild(root);
        root.setLocalTranslation(0, 0, 4);
    }

    /**
     * Entfernt die Hand aus der Oberfläche.
     */
    public void detach() {
        root.removeFromParent();
    }

    /**
     * @param visible Sichtbarkeit
     */
    public void setVisible(boolean visible) {
        root.setCullHint(visible ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
    }

    /**
     * @param interactive {@code false} deaktiviert Klicks (z. B. während eines Dialogs)
     */
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        if (!interactive) hovered = null;
    }

    /**
     * @param playable Prüfung, ob eine Karte gerade gespielt werden kann
     */
    public void setPlayable(Predicate<Card> playable) {
        this.playable = playable;
        refreshGlow();
    }

    /**
     * Färbt spielbare Karten neu ein.
     */
    public void refreshGlow() {
        for (CardSprite s : sprites) {
            if (s.placeholder) continue;
            boolean can = s.getCard() != null && playable.test(s.getCard());
            s.setGlow(can ? Theme.withAlpha(Theme.GOLD, 0.85f) : null, true);
            s.setBrightness(can || !anyPlayable() ? 1f : 0.82f);
        }
    }

    private boolean anyPlayable() {
        return sprites.stream().anyMatch(s -> !s.placeholder && s.getCard() != null && playable.test(s.getCard()));
    }

    /**
     * @return Karten in der aktuellen Reihenfolge
     */
    public List<Card> cards() {
        List<Card> list = new ArrayList<>();
        for (CardSprite s : sprites) if (!s.placeholder) list.add(s.getCard());
        return list;
    }

    /**
     * Gleicht die Hand ohne Flug-Animation mit dem Modell ab.
     *
     * @param hand Karten laut Modell
     */
    public void sync(List<Card> hand) {
        List<Card> wanted = new ArrayList<>(hand);
        for (CardSprite s : new ArrayList<>(sprites)) {
            if (s.placeholder) {
                reveal(s);
            }
        }
        for (CardSprite s : new ArrayList<>(sprites)) {
            if (!wanted.remove(s.getCard())) remove(s);
        }
        for (Card c : wanted) {
            CardSprite s = add(c);
            s.y = -150;
        }
        refreshGlow();
    }

    private CardSprite add(Card card) {
        CardSprite s = new CardSprite(factory, card, CARD_WIDTH);
        s.x = UiScale.width(cam) / 2;
        s.y = BASE_Y;
        sprites.add(s);
        root.attachChild(s);
        MouseEventControl.addListenersToSpatial(s, mouse);
        layoutTargets();
        return s;
    }

    /**
     * Reserviert einen Platz für eine Karte, die gleich in die Hand fliegt.
     *
     * @param card Karte
     * @return unsichtbarer Platzhalter
     */
    public CardSprite reserve(Card card) {
        CardSprite s = add(card);
        s.placeholder = true;
        s.setAlpha(0);
        layoutTargets();
        s.x = s.tx;
        s.y = s.ty;
        s.rot = s.trot;
        return s;
    }

    /**
     * Macht einen Platzhalter sichtbar.
     *
     * @param s Platzhalter
     */
    public void reveal(CardSprite s) {
        if (!s.placeholder) return;
        s.placeholder = false;
        s.setAlpha(1);
        s.scale = 1.15f;
        refreshGlow();
    }

    /**
     * Nimmt eine Karte aus der Hand (z. B. beim Ausspielen).
     *
     * @param card Karte
     * @return entfernte Darstellung (noch in der Oberfläche) oder {@code null}
     */
    public CardSprite take(Card card) {
        CardSprite found = null;
        for (int i = sprites.size() - 1; i >= 0; i--) {
            CardSprite s = sprites.get(i);
            if (!s.placeholder && s.getCard() != null && s.getCard().equals(card)) {
                found = s;
                if (s == hovered) break;
            }
        }
        if (found == null) return null;
        sprites.remove(found);
        if (hovered == found) hovered = null;
        found.removeControl(MouseEventControl.class);
        layoutTargets();
        return found;
    }

    private void remove(CardSprite s) {
        sprites.remove(s);
        if (hovered == s) hovered = null;
        s.removeControl(MouseEventControl.class);
        s.ty = s.y - 220;
        leaving.add(s);
    }

    /**
     * Rechnet eine Position der Hand in Bildschirmpixel um.
     *
     * @param s Karte der Hand
     * @return Bildschirmposition der Kartenmitte (Zielposition)
     */
    public Vector3f screenTarget(CardSprite s) {
        float k = UiScale.of(cam);
        return new Vector3f(s.tx * k, s.ty * k, 0);
    }

    /**
     * @param s Karte der Hand
     * @return aktuelle Bildschirmposition der Kartenmitte
     */
    public Vector3f screenPosition(CardSprite s) {
        float k = UiScale.of(cam);
        return new Vector3f(s.x * k, s.y * k, 0);
    }

    /**
     * @return Kartenhöhe in Bildschirmpixeln
     */
    public float screenCardHeight() {
        return CARD_WIDTH * CardSprite.RATIO * UiScale.of(cam);
    }

    private void layoutTargets() {
        int n = sprites.size();
        float cx = UiScale.width(cam) / 2;
        float spacing = Math.min(118f, 980f / Math.max(1, n));
        float angle = Math.min(5.5f, 44f / Math.max(1, n));
        int hoverIndex = hovered == null ? -1 : sprites.indexOf(hovered);
        for (int i = 0; i < n; i++) {
            CardSprite s = sprites.get(i);
            float offset = i - (n - 1) / 2f;
            float push = 0;
            if (hoverIndex >= 0 && i != hoverIndex) {
                float d = i - hoverIndex;
                push = Math.signum(d) * 55f / Math.max(1f, Math.abs(d));
            }
            s.tx = cx + offset * spacing + push;
            s.ty = BASE_Y - offset * offset * 2.2f;
            s.trot = -offset * angle * FastMath.DEG_TO_RAD;
            s.tscale = 1f;
            s.tz = i * 0.6f;
            if (i == hoverIndex) {
                s.ty = HOVER_Y;
                s.trot = 0;
                s.tscale = HOVER_SCALE;
                s.tz = 40;
            }
        }
    }

    /**
     * Bewegt die Karten weich zu ihren Zielen.
     *
     * @param tpf Zeit seit dem letzten Bild
     */
    public void update(float tpf) {
        root.setLocalScale(UiScale.of(cam));
        layoutTargets();
        float k = 1 - FastMath.exp(-tpf * 14f);
        for (CardSprite s : sprites) {
            s.x += (s.tx - s.x) * k;
            s.y += (s.ty - s.y) * k;
            s.rot += (s.trot - s.rot) * k;
            s.scale += (s.tscale - s.scale) * k;
            apply(s);
            s.tick(tpf);
        }
        for (CardSprite s : new ArrayList<>(leaving)) {
            s.y += (s.ty - s.y) * k;
            s.setAlpha(s.getAlpha() - tpf * 3f);
            apply(s);
            if (s.getAlpha() <= 0) {
                s.removeFromParent();
                leaving.remove(s);
            }
        }
    }

    private static void apply(CardSprite s) {
        s.setLocalTranslation(s.x, s.y, s.tz);
        s.setLocalRotation(s.getLocalRotation().fromAngles(0, 0, s.rot));
        s.setLocalScale(s.scale);
    }

    /**
     * Leitet Mausereignisse weiter.
     */
    private final class MouseHandler extends DefaultMouseListener {
        @Override
        protected void click(MouseButtonEvent event, Spatial target, Spatial capture) {
            CardSprite s = spriteOf(target);
            if (s == null || s.placeholder || !interactive) return;
            if (event.getButtonIndex() == 1)
                listener.inspect(s.getCard());
            else
                listener.play(s.getCard());
        }

        @Override
        public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture) {
            CardSprite s = spriteOf(target);
            if (s != null && !s.placeholder && interactive) hovered = s;
        }

        @Override
        public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture) {
            CardSprite s = spriteOf(target);
            if (s != null && s == hovered) hovered = null;
        }

        private CardSprite spriteOf(Spatial s) {
            Spatial current = s;
            while (current != null) {
                if (current instanceof CardSprite sprite) return sprite;
                current = current.getParent();
            }
            return null;
        }
    }

    /**
     * Färbt eine Karte kurz rot (z. B. wenn sie nicht gespielt werden kann).
     *
     * @param card Karte
     */
    public void shake(Card card) {
        for (CardSprite s : sprites) {
            if (card.equals(s.getCard())) {
                s.x += 14;
                s.setGlow(new ColorRGBA(1f, 0.25f, 0.2f, 0.9f), false);
            }
        }
    }
}
