package pp.winf2slay.view.game.card;

import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.view.util.Decoration;

/**
 * Eine Karte in der Oberfläche (Hand, Auswahl, Großansicht) – mit Schatten und
 * optionalem Leuchtrahmen. Der Ursprung liegt in der Kartenmitte.
 */
public class CardSprite extends Node {

    /** Seitenverhältnis Höhe/Breite. */
    public static final float RATIO = 1.4f;

    private final Card card;
    private final float width;
    private final float height;
    private final Material faceMat;
    private final Material glowMat;
    private final Material shadowMat;
    private final Geometry glow;
    private ColorRGBA glowColor;
    private boolean pulse;
    private float alpha = 1f;
    private float brightness = 1f;
    private float time;

    /** Aktuelle und Zielwerte für weiche Bewegungen (von der Hand gesteuert). */
    float x;
    float y;
    float rot;
    float scale = 1f;
    float tx;
    float ty;
    float trot;
    float tscale = 1f;
    float tz;
    boolean placeholder;

    /**
     * @param factory Kartenfabrik
     * @param card    Karte oder {@code null} für eine Rückseite
     * @param width   Breite in Referenzpixeln
     */
    public CardSprite(CardFactory factory, Card card, float width) {
        super(card == null ? "sprite:back" : "sprite:" + card.getName());
        this.card = card;
        this.width = width;
        this.height = width * RATIO;

        Geometry shadow = quad("shadow", width * 1.5f, height * 1.42f);
        shadowMat = unshaded(factory, "fx/card_glow.png", new ColorRGBA(0, 0, 0, 0.75f), BlendMode.Alpha);
        shadow.setMaterial(shadowMat);
        shadow.move(width * 0.04f, -height * 0.05f, -0.3f);
        attachChild(shadow);

        glow = quad("glow", width * 1.55f, height * 1.45f);
        glowMat = unshaded(factory, "fx/card_glow.png", ColorRGBA.White.clone(), BlendMode.AlphaAdditive);
        glow.setMaterial(glowMat);
        glow.move(0, 0, -0.2f);
        glow.setCullHint(CullHint.Always);
        attachChild(glow);

        Geometry face = new Geometry("face", CardMeshes.flat(width, height, width * 0.07f));
        faceMat = new Material(factory.assets(), "Common/MatDefs/Misc/Unshaded.j3md");
        faceMat.setTexture("ColorMap", card == null ? factory.texture(CardFactory.BACK) : factory.front(card));
        faceMat.setColor("Color", ColorRGBA.White.clone());
        faceMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        face.setMaterial(faceMat);
        attachChild(face);
    }

    private static Geometry quad(String name, float w, float h) {
        Geometry g = new Decoration(name, new Quad(w, h));
        g.setLocalTranslation(-w / 2, -h / 2, 0);
        return g;
    }

    private static Material unshaded(CardFactory factory, String texture, ColorRGBA color, BlendMode blend) {
        Material m = new Material(factory.assets(), "Common/MatDefs/Misc/Unshaded.j3md");
        m.setTexture("ColorMap", factory.assets().loadTexture(texture));
        m.setColor("Color", color);
        m.getAdditionalRenderState().setBlendMode(blend);
        return m;
    }

    /**
     * @return Karte oder {@code null} bei verdeckten Karten
     */
    public Card getCard() {
        return card;
    }

    /**
     * @return {@code true}, wenn die Karte leuchtet (spielbar oder Ziel)
     */
    public boolean isGlowing() {
        return glowColor != null;
    }

    /**
     * @return Breite in Referenzpixeln
     */
    public float getWidth() {
        return width;
    }

    /**
     * @return Höhe in Referenzpixeln
     */
    public float getHeight() {
        return height;
    }

    /**
     * @param color Farbe des Leuchtrahmens oder {@code null}
     * @param pulse pulsierend
     */
    public void setGlow(ColorRGBA color, boolean pulse) {
        this.glowColor = color == null ? null : color.clone();
        this.pulse = pulse;
        glow.setCullHint(color == null ? CullHint.Always : CullHint.Inherit);
        refresh();
    }

    /**
     * @param alpha Deckkraft 0..1
     */
    public void setAlpha(float alpha) {
        this.alpha = FastMath.clamp(alpha, 0, 1);
        refresh();
    }

    /**
     * @return Deckkraft
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * @param brightness Helligkeit (1 = normal)
     */
    public void setBrightness(float brightness) {
        this.brightness = brightness;
        refresh();
    }

    /**
     * Für pulsierendes Leuchten pro Bild aufrufen.
     *
     * @param tpf Zeit seit dem letzten Bild
     */
    public void tick(float tpf) {
        time += tpf;
        if (glowColor != null && pulse) refresh();
    }

    private void refresh() {
        faceMat.setColor("Color", new ColorRGBA(brightness, brightness, brightness, alpha));
        shadowMat.setColor("Color", new ColorRGBA(0, 0, 0, 0.7f * alpha));
        if (glowColor != null) {
            float p = pulse ? 0.6f + 0.4f * FastMath.sin(time * 4.5f) : 1f;
            ColorRGBA c = glowColor.mult(p);
            c.a = glowColor.a * alpha;
            glowMat.setColor("Color", c);
        }
    }
}
