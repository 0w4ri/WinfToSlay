package pp.winf2slay.view.game.board;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.view.game.card.CardFactory;
import pp.winf2slay.view.game.card.CardNode;
import pp.winf2slay.view.game.card.CardSize;

/**
 * Ein Kartenstapel: ein Block, dessen Höhe der Kartenanzahl entspricht, mit der
 * obersten Karte darauf (verdeckt oder – beim Ablagestapel – aufgedeckt).
 */
public class DeckView extends Node {

    private static final float CARD_HEIGHT = 0.045f;
    private static final float MAX_HEIGHT = 3.2f;

    private final CardFactory factory;
    private final CardSize size;
    private final boolean monster;
    private final boolean faceUp;
    private final Geometry block;
    private CardNode top;
    private Card topCard;
    private int count = -1;

    /**
     * @param factory Kartenfabrik
     * @param size    Kartenformat
     * @param monster {@code true} für den Monsterstapel (rötliche Rückseite)
     * @param faceUp  {@code true}, wenn die oberste Karte aufgedeckt liegt (Ablagestapel)
     * @param place   Lage auf dem Tisch
     */
    public DeckView(CardFactory factory, CardSize size, boolean monster, boolean faceUp, Transform place) {
        super("deck");
        this.factory = factory;
        this.size = size;
        this.monster = monster;
        this.faceUp = faceUp;
        setLocalTransform(place);
        block = new Geometry("deck-block", new Box(size.getWidth() / 2 - 0.12f, 0.5f, size.getHeight() / 2 - 0.12f));
        Material m = new Material(factory.assets(), "Common/MatDefs/Light/Lighting.j3md");
        m.setBoolean("UseMaterialColors", true);
        ColorRGBA paper = monster ? new ColorRGBA(0.55f, 0.36f, 0.32f, 1f) : new ColorRGBA(0.82f, 0.74f, 0.6f, 1f);
        m.setColor("Diffuse", paper);
        m.setColor("Ambient", paper.mult(0.8f));
        block.setMaterial(m);
        block.setShadowMode(ShadowMode.CastAndReceive);
        attachChild(block);
    }

    /**
     * Aktualisiert Höhe und oberste Karte.
     *
     * @param newCount Anzahl der Karten
     * @param newTop   oberste Karte (nur beim Ablagestapel relevant)
     */
    public void update(int newCount, Card newTop) {
        // Beim Ziehen vom Ablagestapel wird die oberste Karte kurz ausgeblendet; liegt darunter
        // eine gleichnamige Karte, bleibt der Knoten erhalten und muss wieder sichtbar werden.
        if (top != null) top.setCullHint(CullHint.Inherit);
        boolean topChanged = faceUp && (newTop == null ? topCard != null : !newTop.equals(topCard));
        if (newCount == count && !topChanged && (top != null) == (newCount > 0)) return;
        count = newCount;
        float height = stackHeight(newCount);
        block.setCullHint(newCount > 1 ? CullHint.Inherit : CullHint.Always);
        block.setLocalScale(1, Math.max(0.01f, height), 1);
        block.setLocalTranslation(0, height / 2, 0);
        if (newCount <= 0 || (faceUp && newTop == null)) {
            removeTop();
            return;
        }
        if (top == null || topChanged) {
            removeTop();
            topCard = faceUp ? newTop : null;
            top = faceUp ? factory.create(newTop) : factory.createHidden(size, monster);
            attachChild(top);
        }
        top.setLocalTranslation(0, height + CardSize.THICKNESS / 2, 0);
    }

    private void removeTop() {
        if (top != null) top.removeFromParent();
        top = null;
        topCard = null;
    }

    /**
     * @return aktuelle Kartenanzahl
     */
    public int getCount() {
        return Math.max(0, count);
    }

    /**
     * @return Weltposition der Stapeloberseite
     */
    public Vector3f topPosition() {
        return localToWorld(new Vector3f(0, stackHeight(Math.max(1, count)) + 0.1f, 0), null);
    }

    /**
     * @return oberste Karte als Knoten oder {@code null}
     */
    public CardNode getTop() {
        return top;
    }

    private static float stackHeight(int n) {
        return Math.min(MAX_HEIGHT, n * CARD_HEIGHT);
    }

    /**
     * Legt eine angeflogene Karte als neue oberste Karte ab (nur Ablagestapel).
     *
     * @param node Karte; wird übernommen
     */
    public void adoptTop(CardNode node) {
        if (!faceUp || node.getCard() == null) {
            node.removeFromParent();
            return;
        }
        removeTop();
        count = Math.max(1, count + 1);
        float height = stackHeight(count);
        block.setCullHint(count > 1 ? CullHint.Inherit : CullHint.Always);
        block.setLocalScale(1, Math.max(0.01f, height), 1);
        block.setLocalTranslation(0, height / 2, 0);
        top = node;
        topCard = node.getCard();
        node.setHighlight(null, false);
        node.setOpacity(1f);
        attachChild(node);
        node.setLocalRotation(new com.jme3.math.Quaternion());
        node.setLocalScale(1f);
        node.setLocalTranslation(0, height + CardSize.THICKNESS / 2, 0);
    }
}
