package pp.winf2slay.view.game;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import pp.winf2slay.view.anim.Easing;
import pp.winf2slay.view.anim.Tween;
import pp.winf2slay.view.anim.Tweens;
import pp.winf2slay.view.audio.Sfx;
import pp.winf2slay.view.audio.SoundManager;
import pp.winf2slay.view.fx.Fx;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Würfel auf dem Spieltisch: werden geworfen, überschlagen sich, springen auf und
 * zeigen am Ende genau die vom Server gewürfelten Augen.
 */
public class DiceView {

    private static final float SIZE = 1.9f;

    private final AssetManager assets;
    private final Node parent;
    private final SoundManager sounds;
    private final Fx fx;
    private final Random random = new Random();
    private final List<Node> dice = new ArrayList<>();
    private Mesh mesh;

    /**
     * @param assets Asset-Manager
     * @param parent Knoten für die Würfel
     * @param sounds Sounds
     * @param fx     Effekte
     */
    public DiceView(AssetManager assets, Node parent, SoundManager sounds, Fx fx) {
        this.assets = assets;
        this.parent = parent;
        this.sounds = sounds;
        this.fx = fx;
    }

    /**
     * Wirft ein Würfelpaar.
     *
     * @param center Landeplatz (Mitte zwischen beiden Würfeln)
     * @param from   Richtung, aus der geworfen wird (z. B. Platz des Spielers)
     * @param die1   Augen des ersten Würfels
     * @param die2   Augen des zweiten Würfels
     * @param tint   Farbe der Würfel (Spielerfarbe, leicht)
     * @return Tween bis zum Liegenbleiben
     */
    public Tween roll(Vector3f center, Vector3f from, int die1, int die2, ColorRGBA tint) {
        Vector3f dir = center.subtract(from);
        dir.y = 0;
        if (dir.lengthSquared() < 1e-3f) dir.set(0, 0, -1);
        dir.normalizeLocal();
        Vector3f side = dir.cross(Vector3f.UNIT_Y).normalizeLocal();
        Tween a = rollOne(center.add(side.mult(1.8f)).addLocal(dir.mult(-0.5f)), dir, die1, tint, 0f);
        Tween b = rollOne(center.subtract(side.mult(1.8f)).addLocal(dir.mult(0.6f)), dir, die2, tint, 0.06f);
        return Tweens.seq(Tweens.call(() -> sounds.play(Sfx.DICE, 1f, 0.06f)), Tweens.par(a, b),
                          Tweens.call(() -> fx.sparks(center.add(0, 1, 0), tint, 12, 6f)));
    }

    private Tween rollOne(Vector3f land, Vector3f dir, int value, ColorRGBA tint, float delay) {
        Node die = createDie(tint);
        Vector3f start = land.subtract(dir.mult(26f)).addLocal(0, 9f, 0);
        Quaternion end = new Quaternion().fromAngleAxis(random.nextFloat() * FastMath.TWO_PI, Vector3f.UNIT_Y)
                                         .multLocal(faceUp(value));
        Vector3f spinAxis = dir.cross(Vector3f.UNIT_Y).normalizeLocal().negateLocal()
                               .addLocal(random.nextFloat() * 0.4f - 0.2f, 0, random.nextFloat() * 0.4f - 0.2f)
                               .normalizeLocal();
        float turns = 3 + random.nextInt(2);
        return Tweens.seq(
                Tweens.delay(delay),
                Tweens.call(() -> {
                    die.setLocalTranslation(start);
                    parent.attachChild(die);
                    dice.add(die);
                }),
                Tweens.lerp(1.05f, t -> {
                    float move = Easing.OUT_CUBIC.apply(t);
                    Vector3f p = new Vector3f().interpolateLocal(start, land, move);
                    float hop = (1 - Easing.OUT_BOUNCE.apply(t)) * 9f;
                    p.y = SIZE / 2 + hop;
                    die.setLocalTranslation(p);
                    float remaining = 1 - Easing.OUT_CUBIC.apply(t);
                    Quaternion spin = new Quaternion().fromAngleAxis(remaining * turns * FastMath.TWO_PI, spinAxis);
                    die.setLocalRotation(spin.mult(end));
                }));
    }

    /**
     * Lässt alle Würfel ausblenden und entfernt sie.
     *
     * @return Tween
     */
    public Tween clear() {
        return Tweens.defer(() -> {
            List<Node> current = new ArrayList<>(dice);
            dice.clear();
            if (current.isEmpty()) return Tweens.NONE;
            return Tweens.seq(Tweens.lerp(0.3f, Easing.IN_QUAD, t -> {
                for (Node d : current) d.setLocalScale(Math.max(0.01f, 1 - t));
            }), Tweens.call(() -> current.forEach(Node::removeFromParent)));
        });
    }

    /**
     * Entfernt alle Würfel sofort.
     */
    public void clearNow() {
        dice.forEach(Node::removeFromParent);
        dice.clear();
    }

    /**
     * Drehung, bei der die gewünschte Augenzahl oben liegt.
     */
    private static Quaternion faceUp(int value) {
        return switch (value) {
            case 6 -> new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_X);
            case 2 -> new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z);
            case 5 -> new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_Z);
            case 3 -> new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
            case 4 -> new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
            default -> new Quaternion();
        };
    }

    private Node createDie(ColorRGBA tint) {
        if (mesh == null) mesh = buildMesh(SIZE / 2);
        Geometry g = new Geometry("die", mesh);
        Material m = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        TextureKey key = new TextureKey("dice/dice.png", true);
        key.setGenerateMips(true);
        m.setTexture("DiffuseMap", assets.loadTexture(key));
        m.setBoolean("UseMaterialColors", true);
        ColorRGBA c = ColorRGBA.White.clone().interpolateLocal(tint, 0.12f);
        m.setColor("Diffuse", c);
        m.setColor("Ambient", c);
        m.setColor("Specular", new ColorRGBA(0.6f, 0.6f, 0.6f, 1f));
        m.setFloat("Shininess", 48f);
        g.setMaterial(m);
        g.setShadowMode(ShadowMode.CastAndReceive);
        Node node = new Node("die-node");
        node.attachChild(g);
        return node;
    }

    /**
     * Würfel mit eigener Texturkoordinate je Seite (Atlas 3 × 2: oben 1–3, unten 4–6).
     */
    private static Mesh buildMesh(float h) {
        // Normale, u-Achse, v-Achse und Augenzahl je Seite (u × v = Normale)
        float[][] faces = {
                {0, 1, 0, 1, 0, 0, 0, 0, -1, 1},
                {0, -1, 0, 1, 0, 0, 0, 0, 1, 6},
                {1, 0, 0, 0, 0, -1, 0, 1, 0, 2},
                {-1, 0, 0, 0, 0, 1, 0, 1, 0, 5},
                {0, 0, 1, 1, 0, 0, 0, 1, 0, 3},
                {0, 0, -1, -1, 0, 0, 0, 1, 0, 4},
        };
        float[] pos = new float[6 * 4 * 3];
        float[] nor = new float[6 * 4 * 3];
        float[] uv = new float[6 * 4 * 2];
        short[] idx = new short[6 * 6];
        for (int f = 0; f < 6; f++) {
            Vector3f n = new Vector3f(faces[f][0], faces[f][1], faces[f][2]);
            Vector3f u = new Vector3f(faces[f][3], faces[f][4], faces[f][5]);
            Vector3f v = new Vector3f(faces[f][6], faces[f][7], faces[f][8]);
            int value = (int) faces[f][9];
            int col = (value - 1) % 3;
            int row = (value - 1) / 3;
            float u0 = col / 3f;
            float u1 = (col + 1) / 3f;
            float vt = 1 - row / 2f;
            float vb = 1 - (row + 1) / 2f;
            Vector3f c = n.mult(h);
            Vector3f[] corners = {c.subtract(u.mult(h)).subtractLocal(v.mult(h)), c.add(u.mult(h)).subtractLocal(v.mult(h)),
                                  c.add(u.mult(h)).addLocal(v.mult(h)), c.subtract(u.mult(h)).addLocal(v.mult(h))};
            float[][] uvs = {{u0, vb}, {u1, vb}, {u1, vt}, {u0, vt}};
            for (int i = 0; i < 4; i++) {
                int k = f * 4 + i;
                pos[k * 3] = corners[i].x;
                pos[k * 3 + 1] = corners[i].y;
                pos[k * 3 + 2] = corners[i].z;
                nor[k * 3] = n.x;
                nor[k * 3 + 1] = n.y;
                nor[k * 3 + 2] = n.z;
                uv[k * 2] = uvs[i][0];
                uv[k * 2 + 1] = uvs[i][1];
            }
            int b = f * 4;
            short[] tri = {(short) b, (short) (b + 1), (short) (b + 2), (short) b, (short) (b + 2), (short) (b + 3)};
            System.arraycopy(tri, 0, idx, f * 6, 6);
        }
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(pos));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(nor));
        mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(uv));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createShortBuffer(idx));
        mesh.updateBound();
        return mesh;
    }
}
