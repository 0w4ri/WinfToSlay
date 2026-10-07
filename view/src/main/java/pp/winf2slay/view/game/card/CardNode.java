package pp.winf2slay.view.game.card;

import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.view.util.Decoration;

/**
 * Eine Karte auf dem Spieltisch.
 *
 * <p>Aufbau: {@code CardNode} (wird vom Spielfeld und von Animationen bewegt) →
 * {@code pivot} (Hover-Anheben, Wackeln) → Vorderseite, Rückseite, Rand und ein
 * Leuchtrahmen für Hervorhebungen.</p>
 */
public class CardNode extends Node {

    private static final float GLOW_SCALE = 1.45f;

    private final CardSize size;
    private final Card card;
    private final Node pivot = new Node("pivot");
    private final Geometry front;
    private final Geometry back;
    private final Geometry edge;
    private final Geometry glow;
    private final Material frontMat;
    private final Material backMat;
    private final Material edgeMat;
    private final Material glowMat;
    private ColorRGBA highlight;
    private boolean pulsing;
    private float opacity = 1f;
    private float hoverLift;
    private float hoverTarget;
    private float time;

    CardNode(CardFactory factory, CardSize size, Card card) {
        super(card == null ? "card:hidden" : "card:" + card.getName());
        this.size = size;
        this.card = card;
        attachChild(pivot);

        frontMat = factory.cardMaterial(null);
        backMat = factory.cardMaterial(factory.backTexture());
        backMat.setColor("Diffuse", CardFactory.backTint(CardFactory.isMonster(card)));
        edgeMat = factory.edgeMaterial();

        front = new Geometry("front", CardMeshes.front(size));
        front.setMaterial(frontMat);
        back = new Geometry("back", CardMeshes.back(size));
        back.setMaterial(backMat);
        edge = new Geometry("edge", CardMeshes.edge(size));
        edge.setMaterial(edgeMat);
        for (Geometry g : new Geometry[]{front, back, edge}) {
            g.setShadowMode(ShadowMode.CastAndReceive);
            pivot.attachChild(g);
        }

        float gw = size.getWidth() * GLOW_SCALE;
        float gh = size.getHeight() * 1.32f;
        glow = new Decoration("glow", new Quad(gw, gh));
        glowMat = new Material(factory.assets(), "Common/MatDefs/Misc/Unshaded.j3md");
        glowMat.setTexture("ColorMap", factory.assets().loadTexture("fx/card_glow.png"));
        glowMat.setColor("Color", ColorRGBA.White.clone());
        glowMat.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
        glowMat.getAdditionalRenderState().setDepthWrite(false);
        glow.setMaterial(glowMat);
        glow.setQueueBucket(Bucket.Transparent);
        glow.setShadowMode(ShadowMode.Off);
        glow.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        glow.setLocalTranslation(-gw / 2, -CardSize.THICKNESS, gh / 2);
        glow.setCullHint(CullHint.Always);
        glow.setBatchHint(BatchHint.Never);
        pivot.attachChild(glow);

        addControl(new CardControl());
    }

    /**
     * @return Karte oder {@code null} bei verdeckten Karten
     */
    public Card getCard() {
        return card;
    }

    /**
     * @return Format
     */
    public CardSize getSize() {
        return size;
    }

    /**
     * @return innerer Knoten für Hover- und Wackelbewegungen
     */
    public Node getPivot() {
        return pivot;
    }

    void setFrontTexture(Texture texture) {
        frontMat.setTexture("DiffuseMap", texture);
    }

    void tintBack(ColorRGBA tint) {
        backMat.setColor("Diffuse", tint.clone());
        frontMat.setColor("Diffuse", tint.clone());
    }

    /**
     * Hebt die Karte farbig hervor.
     *
     * @param color Farbe oder {@code null} zum Ausschalten
     * @param pulse {@code true} für pulsierendes Leuchten
     */
    public void setHighlight(ColorRGBA color, boolean pulse) {
        this.highlight = color == null ? null : color.clone();
        this.pulsing = pulse;
        glow.setCullHint(color == null ? CullHint.Always : CullHint.Inherit);
        if (color == null) {
            frontMat.clearParam("GlowColor");
        }
        updateGlow();
    }

    /**
     * @return aktuelle Hervorhebung oder {@code null}
     */
    public ColorRGBA getHighlight() {
        return highlight;
    }

    /**
     * Lässt die Karte beim Überfahren mit der Maus leicht abheben.
     *
     * @param hovered {@code true}, solange die Maus über der Karte ist
     */
    public void setHovered(boolean hovered) {
        hoverTarget = hovered ? 1f : 0f;
    }

    /**
     * Setzt die Deckkraft (z. B. zum Ausblenden zerstörter Karten).
     *
     * @param alpha Deckkraft 0..1
     */
    public void setOpacity(float alpha) {
        float a = FastMath.clamp(alpha, 0, 1);
        if (a == opacity) return;
        opacity = a;
        boolean transparent = a < 0.999f;
        for (Geometry g : new Geometry[]{front, back, edge}) {
            Material m = g.getMaterial();
            m.getAdditionalRenderState().setBlendMode(transparent ? BlendMode.Alpha : BlendMode.Off);
            ColorRGBA d = m.getParamValue("Diffuse");
            ColorRGBA copy = d.clone();
            copy.a = a;
            m.setColor("Diffuse", copy);
            g.setQueueBucket(transparent ? Bucket.Transparent : Bucket.Inherit);
            g.setShadowMode(transparent ? ShadowMode.Off : ShadowMode.CastAndReceive);
        }
        updateGlow();
    }

    /**
     * @return Deckkraft
     */
    public float getOpacity() {
        return opacity;
    }

    /**
     * Verdunkelt oder erhellt die Karte (z. B. benutzte Helden).
     *
     * @param brightness 1 = normal, kleiner = dunkler
     */
    public void setBrightness(float brightness) {
        ColorRGBA c = new ColorRGBA(brightness, brightness, brightness, opacity);
        frontMat.setColor("Diffuse", c);
        frontMat.setColor("Ambient", c);
    }

    private void updateGlow() {
        if (highlight == null) return;
        float pulse = pulsing ? 0.65f + 0.35f * FastMath.sin(time * 4.2f) : 1f;
        ColorRGBA c = highlight.mult(pulse);
        c.a = highlight.a * opacity;
        glowMat.setColor("Color", c);
        frontMat.setColor("GlowColor", highlight.mult(0.18f * pulse * opacity));
    }

    /**
     * Steuert Hover-Bewegung und Pulsieren.
     */
    private final class CardControl extends AbstractControl {
        @Override
        protected void controlUpdate(float tpf) {
            time += tpf;
            if (hoverLift != hoverTarget) {
                float step = tpf * 7f;
                hoverLift = hoverLift < hoverTarget ? Math.min(hoverTarget, hoverLift + step)
                                                    : Math.max(hoverTarget, hoverLift - step);
                float eased = hoverLift * hoverLift * (3 - 2 * hoverLift);
                pivot.setLocalTranslation(0, eased * 1.2f, 0);
                pivot.setLocalScale(1f + eased * 0.06f);
            }
            if (highlight != null && pulsing) updateGlow();
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            // nichts zu tun
        }
    }
}
