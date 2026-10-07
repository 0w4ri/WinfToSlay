package pp.winf2slay.view.fx;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.BufferUtils;
import pp.winf2slay.view.anim.Animator;
import pp.winf2slay.view.anim.Easing;
import pp.winf2slay.view.anim.Tween;
import pp.winf2slay.view.anim.Tweens;
import pp.winf2slay.view.audio.Sfx;
import pp.winf2slay.view.audio.SoundManager;
import pp.winf2slay.view.game.CameraRig;
import pp.winf2slay.view.game.card.CardNode;
import pp.winf2slay.view.ui.Theme;
import pp.winf2slay.view.ui.UiScale;
import pp.winf2slay.view.util.Decoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Bibliothek der Spezialeffekte.
 *
 * <p>Jede Methode startet einen Effekt und liefert einen {@link Tween}, dessen
 * Dauer dem „spürbaren“ Teil entspricht (z. B. bis zum Einschlag). Partikel, die
 * danach noch ausklingen, räumen sich selbst auf und halten das Spiel nicht auf.</p>
 */
public class Fx {

    private static final ColorRGBA CLEAR = new ColorRGBA(0, 0, 0, 0);

    private final AssetManager assets;
    private final Node world;
    private final Node overlay;
    private final Camera cam;
    private final CameraRig rig;
    private final SoundManager sounds;
    private final Animator animator;
    private final Theme theme;
    private final Particles particles;
    private final Random random = new Random();

    /**
     * @param assets   Asset-Manager
     * @param world    3D-Knoten für Effekte
     * @param overlay  Oberflächenknoten (Bildschirmpixel) für 2D-Effekte
     * @param cam      Kamera
     * @param rig      Kamerasteuerung (Wackeln, Fokus)
     * @param sounds   Sounds
     * @param animator Animationen
     * @param theme    Schriften
     */
    public Fx(AssetManager assets, Node world, Node overlay, Camera cam, CameraRig rig, SoundManager sounds,
              Animator animator, Theme theme) {
        this.assets = assets;
        this.world = world;
        this.overlay = overlay;
        this.cam = cam;
        this.rig = rig;
        this.sounds = sounds;
        this.animator = animator;
        this.theme = theme;
        this.particles = new Particles(assets);
    }

    /**
     * @param density Partikeldichte (1 = voll)
     */
    public void setDensity(float density) {
        particles.setDensity(density);
    }

    /**
     * @return Partikelbaukasten
     */
    public Particles particles() {
        return particles;
    }

    // ------------------------------------------------------------------
    // Grundbausteine
    // ------------------------------------------------------------------

    /**
     * Stößt einen Emitter einmalig aus und entfernt ihn nach seiner Lebensdauer.
     *
     * @param emitter Emitter
     * @param pos     Position
     */
    public void emit(ParticleEmitter emitter, Vector3f pos) {
        emitter.setLocalTranslation(pos);
        world.attachChild(emitter);
        emitter.emitAllParticles();
        float life = emitter.getHighLife() + 0.2f;
        animator.play(Tweens.seq(Tweens.delay(life), Tweens.call(emitter::removeFromParent)));
    }

    private Material unshaded(String texture, ColorRGBA color, boolean additive) {
        Material m = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        if (texture != null) m.setTexture("ColorMap", assets.loadTexture(texture));
        m.setColor("Color", color.clone());
        m.getAdditionalRenderState().setBlendMode(additive ? BlendMode.AlphaAdditive : BlendMode.Alpha);
        m.getAdditionalRenderState().setDepthWrite(false);
        m.getAdditionalRenderState().setFaceCullMode(com.jme3.material.RenderState.FaceCullMode.Off);
        return m;
    }

    /**
     * Erzeugt eine zur Kamera ausgerichtete Fläche.
     */
    private Node billboard(String texture, float size, ColorRGBA color, boolean additive) {
        Node node = new Node("billboard");
        Geometry g = new Decoration("billboard-quad", new Quad(size, size));
        g.setMaterial(unshaded(texture, color, additive));
        g.setQueueBucket(Bucket.Transparent);
        g.setShadowMode(ShadowMode.Off);
        g.setLocalTranslation(-size / 2, -size / 2, 0);
        node.attachChild(g);
        node.addControl(new BillboardControl());
        return node;
    }

    /**
     * Erzeugt eine flach auf dem Tisch liegende Fläche.
     */
    private Geometry flat(String texture, float size, ColorRGBA color, boolean additive) {
        Geometry g = new Decoration("flat", new Quad(size, size));
        g.setMaterial(unshaded(texture, color, additive));
        g.setQueueBucket(Bucket.Transparent);
        g.setShadowMode(ShadowMode.Off);
        return g;
    }

    private static void setColor(Spatial s, ColorRGBA c) {
        if (s instanceof Geometry g) {
            g.getMaterial().setColor("Color", c);
        }
        else if (s instanceof Node n) {
            for (Spatial child : n.getChildren()) setColor(child, c);
        }
    }

    // ------------------------------------------------------------------
    // Partikel-Effekte
    // ------------------------------------------------------------------

    /**
     * Funkenregen.
     *
     * @param pos   Ort
     * @param color Farbe
     * @param count Anzahl
     * @param speed Geschwindigkeit
     */
    public void sparks(Vector3f pos, ColorRGBA color, int count, float speed) {
        ColorRGBA end = color.clone();
        end.a = 0;
        emit(particles.builder("fx/spark.png", count).colors(color.clone(), end).sizes(1.3f, 0.2f)
                      .life(0.35f, 0.9f).velocity(0, speed, 0, 1f).gravity(0, speed * 1.4f, 0).sphere(0.6f)
                      .additive().build(), pos);
    }

    /**
     * Rauchwolke.
     *
     * @param pos  Ort
     * @param size Größe
     * @param dark {@code true} für dunklen Rauch
     */
    public void smoke(Vector3f pos, float size, boolean dark) {
        ColorRGBA start = dark ? new ColorRGBA(0.34f, 0.3f, 0.27f, 0.5f) : new ColorRGBA(0.78f, 0.75f, 0.7f, 0.42f);
        ColorRGBA end = start.clone();
        end.a = 0;
        emit(particles.builder("fx/smoke.png", 18).atlas().colors(start, end).sizes(size * 0.7f, size * 2.4f)
                      .life(1.2f, 2.4f).velocity(0, 3.5f, 0, 0.8f).gravity(0, -1.2f, 0).sphere(size * 0.25f)
                      .spin(0.6f).build(), pos);
    }

    /**
     * Glitzernde Sterne (z. B. für Trophäen und Erfolge).
     *
     * @param pos   Ort
     * @param color Farbe
     */
    public void twinkle(Vector3f pos, ColorRGBA color) {
        ColorRGBA end = color.clone();
        end.a = 0;
        emit(particles.builder("fx/spark.png", 26).colors(color.clone(), end).sizes(2.2f, 0.2f).life(0.6f, 1.3f)
                      .velocity(0, 4f, 0, 1f).gravity(0, -1.5f, 0).sphere(3.5f).spin(2f).additive().build(), pos);
    }

    /**
     * Glühende Asche, die von einer verbrennenden Karte aufsteigt.
     *
     * @param pos Ort
     */
    public void embers(Vector3f pos) {
        emit(particles.builder("fx/glow.png", 40).colors(new ColorRGBA(1f, 0.6f, 0.15f, 1f),
                                                         new ColorRGBA(0.6f, 0.1f, 0f, 0f))
                      .sizes(0.9f, 0.1f).life(0.8f, 1.8f).velocity(0, 5f, 0, 0.9f).gravity(0, -2.5f, 0)
                      .sphere(3f).additive().build(), pos);
        emit(particles.builder("fx/flame.png", 14).atlas().colors(new ColorRGBA(1f, 0.55f, 0.1f, 0.9f),
                                                                  new ColorRGBA(0.8f, 0.1f, 0f, 0f))
                      .sizes(3f, 1f).life(0.4f, 0.8f).velocity(0, 6f, 0, 0.5f).gravity(0, -3f, 0).sphere(2f)
                      .additive().build(), pos);
    }

    /**
     * Startet eine Funkenspur, die einer fliegenden Karte folgt.
     *
     * @param card  Karte
     * @param color Farbe
     * @return Emitter (mit {@link #stopTrail(ParticleEmitter)} beenden)
     */
    public ParticleEmitter trail(CardNode card, ColorRGBA color) {
        ColorRGBA end = color.clone();
        end.a = 0;
        ParticleEmitter e = particles.builder("fx/glow.png", 60).colors(color.clone(), end).sizes(1.4f, 0.1f)
                                     .life(0.3f, 0.6f).velocity(0, 0.5f, 0, 1f).gravity(0, 0, 0).sphere(1.5f)
                                     .additive().rate(80).build();
        card.attachChild(e);
        return e;
    }

    /**
     * Beendet eine Funkenspur weich.
     *
     * @param trail Emitter
     */
    public void stopTrail(ParticleEmitter trail) {
        if (trail == null) return;
        trail.setParticlesPerSec(0);
        animator.play(Tweens.seq(Tweens.delay(0.7f), Tweens.call(trail::removeFromParent)));
    }

    // ------------------------------------------------------------------
    // Licht- und Formeffekte
    // ------------------------------------------------------------------

    /**
     * Kurzes Aufleuchten an einer Stelle.
     *
     * @param pos     Ort
     * @param size    Größe
     * @param color   Farbe
     * @param seconds Dauer
     * @return Tween
     */
    public Tween flash(Vector3f pos, float size, ColorRGBA color, float seconds) {
        Node flash = billboard("fx/glow.png", size, color, true);
        flash.setLocalTranslation(pos);
        return Tweens.seq(
                Tweens.call(() -> world.attachChild(flash)),
                Tweens.lerp(seconds, Easing.OUT_QUAD, t -> {
                    flash.setLocalScale(0.6f + t * 0.8f);
                    ColorRGBA c = color.clone();
                    c.a = color.a * (1 - t);
                    setColor(flash, c);
                }),
                Tweens.call(flash::removeFromParent));
    }

    /**
     * Ringförmige Druckwelle auf dem Tisch.
     *
     * @param pos     Mittelpunkt
     * @param radius  Endradius
     * @param color   Farbe
     * @param seconds Dauer
     * @return Tween
     */
    public Tween shockwave(Vector3f pos, float radius, ColorRGBA color, float seconds) {
        Geometry ring = flat("fx/ring.png", 1f, color, true);
        Node holder = new Node("shockwave");
        ring.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        ring.setLocalTranslation(-0.5f, 0, 0.5f);
        holder.attachChild(ring);
        holder.setLocalTranslation(pos.x, pos.y + 0.3f, pos.z);
        return Tweens.seq(
                Tweens.call(() -> world.attachChild(holder)),
                Tweens.lerp(seconds, Easing.OUT_CUBIC, t -> {
                    float d = Math.max(0.01f, radius * 2 * t);
                    holder.setLocalScale(d, 1, d);
                    ColorRGBA c = color.clone();
                    c.a = color.a * (1 - t * t);
                    ring.getMaterial().setColor("Color", c);
                }),
                Tweens.call(holder::removeFromParent));
    }

    /**
     * Rotierender magischer Kreis unter einer Karte.
     *
     * @param pos     Mittelpunkt
     * @param size    Durchmesser
     * @param color   Farbe
     * @param seconds Dauer
     * @return Tween
     */
    public Tween magicCircle(Vector3f pos, float size, ColorRGBA color, float seconds) {
        Geometry circle = flat("fx/magic_circle.png", size, color, true);
        circle.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        circle.setLocalTranslation(-size / 2, 0, size / 2);
        Node spin = new Node("magic-spin");
        spin.attachChild(circle);
        Node holder = new Node("magic");
        holder.attachChild(spin);
        holder.setLocalTranslation(pos.x, pos.y + 0.15f, pos.z);
        return Tweens.seq(
                Tweens.call(() -> world.attachChild(holder)),
                Tweens.lerp(seconds, t -> {
                    float in = Math.min(1, t * 5);
                    float out = Math.min(1, (1 - t) * 4);
                    holder.setLocalScale(0.4f + 0.6f * Easing.OUT_BACK.apply(in));
                    spin.setLocalRotation(new Quaternion().fromAngleAxis(t * seconds * 1.6f, Vector3f.UNIT_Y));
                    ColorRGBA c = color.clone();
                    c.a = color.a * Math.min(in, out);
                    circle.getMaterial().setColor("Color", c);
                }),
                Tweens.call(holder::removeFromParent));
    }

    /**
     * Senkrechte Lichtsäule.
     *
     * @param pos     Fußpunkt
     * @param height  Höhe
     * @param color   Farbe
     * @param seconds Dauer
     * @return Tween
     */
    public Tween beam(Vector3f pos, float height, ColorRGBA color, float seconds) {
        Geometry g = new Decoration("beam", new Quad(8f, height));
        g.setMaterial(unshaded("fx/beam.png", color, true));
        g.setQueueBucket(Bucket.Transparent);
        g.setLocalTranslation(-4f, 0, 0);
        Node node = new Node("beam");
        node.attachChild(g);
        BillboardControl bb = new BillboardControl();
        bb.setAlignment(BillboardControl.Alignment.AxialY);
        node.addControl(bb);
        node.setLocalTranslation(pos);
        return Tweens.seq(
                Tweens.call(() -> world.attachChild(node)),
                Tweens.lerp(seconds, t -> {
                    float a = Easing.PULSE.apply(t);
                    node.setLocalScale(0.4f + a * 0.8f, 1, 1);
                    ColorRGBA c = color.clone();
                    c.a = color.a * a;
                    g.getMaterial().setColor("Color", c);
                }),
                Tweens.call(node::removeFromParent));
    }

    /**
     * Krallenspuren (Monsterangriff).
     *
     * @param pos   Ort
     * @param color Farbe
     * @return Tween
     */
    public Tween slash(Vector3f pos, ColorRGBA color) {
        Node node = billboard("fx/slash.png", 18f, color, true);
        node.setLocalTranslation(pos.add(0, 4, 0));
        return Tweens.seq(
                Tweens.call(() -> {
                    world.attachChild(node);
                    sounds.play(Sfx.MONSTER_ATTACK, 0.8f, 0.05f);
                }),
                Tweens.lerp(0.5f, Easing.OUT_CUBIC, t -> {
                    node.setLocalScale(0.5f + t * 0.7f);
                    ColorRGBA c = color.clone();
                    c.a = color.a * (t < 0.3f ? t / 0.3f : 1 - (t - 0.3f) / 0.7f);
                    setColor(node, c);
                }),
                Tweens.call(node::removeFromParent));
    }

    /**
     * Gekreuzte Schwerter, die zusammenprallen (Herausforderung).
     *
     * @param pos Ort
     * @return Tween
     */
    public Tween swords(Vector3f pos) {
        Node node = billboard("fx/swords.png", 12f, ColorRGBA.White, false);
        node.setLocalTranslation(pos.add(0, 9, 0));
        return Tweens.seq(
                Tweens.call(() -> world.attachChild(node)),
                Tweens.lerp(0.35f, Easing.OUT_BACK, t -> node.setLocalScale(Math.max(0.01f, t * 1.15f))),
                Tweens.call(() -> {
                    sounds.play(Sfx.SWORDS);
                    sparks(pos.add(0, 9, 0), new ColorRGBA(1f, 0.85f, 0.4f, 1f), 50, 18f);
                    rig.shake(0.6f, 0.25f);
                }),
                Tweens.lerp(0.18f, t -> node.setLocalScale(1.15f - 0.15f * t)),
                Tweens.delay(0.55f),
                Tweens.lerp(0.25f, Easing.IN_QUAD, t -> {
                    node.setLocalScale(1 - t);
                    setColor(node, new ColorRGBA(1, 1, 1, 1 - t));
                }),
                Tweens.call(node::removeFromParent));
    }

    /**
     * Explosion mit Feuerball, Rauch, Trümmern, Druckwelle und Kamerawackeln.
     *
     * @param pos   Ort
     * @param scale Größe (1 = Mörser, 2,5 = Bombe)
     * @return Tween bis kurz nach dem Knall
     */
    public Tween explosion(Vector3f pos, float scale) {
        return Tweens.seq(
                Tweens.call(() -> {
                    sounds.play(scale > 1.5f ? Sfx.EXPLOSION_BIG : Sfx.EXPLOSION, 1f, 0.06f);
                    rig.shake(1.2f * scale, 0.45f + 0.25f * scale);
                    emit(particles.builder("fx/explosion.png", (int) (24 * scale))
                                  .colors(new ColorRGBA(1f, 0.9f, 0.6f, 1f), new ColorRGBA(0.6f, 0.15f, 0.02f, 0f))
                                  .sizes(4f * scale, 9f * scale).life(0.35f, 0.75f)
                                  .velocity(0, 6f * scale, 0, 1f).gravity(0, -2f, 0).sphere(1.5f * scale).spin(1f)
                                  .additive().build(), pos.add(0, 1.5f, 0));
                    emit(particles.builder("fx/debris.png", (int) (26 * scale)).atlas()
                                  .colors(new ColorRGBA(0.25f, 0.18f, 0.12f, 1f), new ColorRGBA(0.2f, 0.15f, 0.1f, 0f))
                                  .sizes(0.8f, 0.6f).life(0.8f, 1.6f).velocity(0, 22f * Math.min(1.6f, scale), 0, 0.9f)
                                  .gravity(0, 40f, 0).sphere(1f).spin(6f).build(), pos.add(0, 1, 0));
                    sparks(pos.add(0, 1.5f, 0), new ColorRGBA(1f, 0.75f, 0.3f, 1f), (int) (40 * scale), 20f);
                    smoke(pos.add(0, 2, 0), 6f * scale, true);
                }),
                Tweens.par(flash(pos.add(0, 3, 0), 26f * scale, new ColorRGBA(1f, 0.85f, 0.55f, 1f), 0.45f),
                           shockwave(pos, 16f * scale, new ColorRGBA(1f, 0.7f, 0.35f, 0.9f), 0.7f)));
    }

    /**
     * Blitz vom Himmel auf ein Ziel.
     *
     * @param target Einschlagpunkt
     * @param color  Farbe
     * @return Tween
     */
    public Tween lightning(Vector3f target, ColorRGBA color) {
        // Start über der Tischmitte zur Kamera hin versetzt, damit der Blitz gut sichtbar ist
        Vector3f from = new Vector3f(target.x * 0.4f + random.nextFloat() * 8 - 4, 58,
                                     target.z * 0.4f + 12 + random.nextFloat() * 6);
        List<Geometry> bolts = new ArrayList<>();
        Node node = new Node("lightning");
        Material mat = unshaded("fx/beam.png", color, true);
        for (int i = 0; i < 2; i++) {
            Geometry g = new Decoration("bolt", boltMesh(from, target, i == 0 ? 2.4f : 1.2f));
            g.setMaterial(mat);
            g.setQueueBucket(Bucket.Transparent);
            bolts.add(g);
            node.attachChild(g);
        }
        return Tweens.seq(
                Tweens.call(() -> {
                    world.attachChild(node);
                    sounds.play(Sfx.THUNDER, 1f, 0.08f);
                    rig.shake(0.9f, 0.35f);
                    sparks(target.add(0, 1, 0), color, 50, 16f);
                    smoke(target.add(0, 1, 0), 4f, false);
                }),
                Tweens.par(flash(target.add(0, 2, 0), 22f, color, 0.4f), screenFlash(color, 0.18f, 0.35f),
                           Tweens.lerp(0.6f, t -> {
                               boolean visible = t < 0.2f || (t > 0.28f && t < 0.5f) || (t > 0.58f && t < 0.8f);
                               node.setCullHint(visible ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
                               if (t > 0.24f && t < 0.26f) {
                                   for (int i = 0; i < bolts.size(); i++)
                                       bolts.get(i).setMesh(boltMesh(from, target, i == 0 ? 2.4f : 1.2f));
                               }
                           })),
                Tweens.call(node::removeFromParent));
    }

    private Mesh boltMesh(Vector3f from, Vector3f to, float width) {
        int segments = 14;
        List<Vector3f> points = new ArrayList<>();
        Vector3f dir = to.subtract(from);
        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            Vector3f p = from.add(dir.mult(t));
            if (i > 0 && i < segments) {
                float amp = 4.5f * FastMath.sin(t * FastMath.PI);
                p.addLocal((random.nextFloat() * 2 - 1) * amp, 0, (random.nextFloat() * 2 - 1) * amp);
            }
            points.add(p);
        }
        Vector3f camPos = cam.getLocation();
        float[] pos = new float[points.size() * 2 * 3];
        float[] uv = new float[points.size() * 2 * 2];
        short[] idx = new short[segments * 6];
        for (int i = 0; i < points.size(); i++) {
            Vector3f p = points.get(i);
            Vector3f along = (i < points.size() - 1 ? points.get(i + 1).subtract(p) : p.subtract(points.get(i - 1)));
            Vector3f side = along.cross(camPos.subtract(p)).normalizeLocal().multLocal(width);
            Vector3f a = p.subtract(side);
            Vector3f b = p.add(side);
            pos[i * 6] = a.x;
            pos[i * 6 + 1] = a.y;
            pos[i * 6 + 2] = a.z;
            pos[i * 6 + 3] = b.x;
            pos[i * 6 + 4] = b.y;
            pos[i * 6 + 5] = b.z;
            float v = 0.15f + 0.7f * i / segments;
            uv[i * 4] = 0;
            uv[i * 4 + 1] = v;
            uv[i * 4 + 2] = 1;
            uv[i * 4 + 3] = v;
        }
        for (int i = 0; i < segments; i++) {
            int a = i * 2;
            idx[i * 6] = (short) a;
            idx[i * 6 + 1] = (short) (a + 1);
            idx[i * 6 + 2] = (short) (a + 2);
            idx[i * 6 + 3] = (short) (a + 1);
            idx[i * 6 + 4] = (short) (a + 3);
            idx[i * 6 + 5] = (short) (a + 2);
        }
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(pos));
        mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(uv));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createShortBuffer(idx));
        mesh.updateBound();
        return mesh;
    }

    // ------------------------------------------------------------------
    // Waffen (Zauberkarten)
    // ------------------------------------------------------------------

    private Material solid(ColorRGBA color) {
        Material m = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", color);
        m.setColor("Ambient", color.mult(0.6f));
        m.setColor("Specular", ColorRGBA.White.mult(0.4f));
        m.setFloat("Shininess", 30f);
        return m;
    }

    private Node shell(float scale) {
        Node shell = new Node("shell");
        Geometry body = new Geometry("shell-body", new Cylinder(6, 12, 0.6f * scale, 2.6f * scale, true));
        body.setMaterial(solid(new ColorRGBA(0.25f, 0.28f, 0.2f, 1f)));
        body.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        shell.attachChild(body);
        Geometry tip = new Geometry("shell-tip", new Sphere(8, 12, 0.6f * scale));
        tip.setMaterial(solid(new ColorRGBA(0.6f, 0.45f, 0.15f, 1f)));
        tip.setLocalTranslation(0, -1.3f * scale, 0);
        shell.attachChild(tip);
        for (int i = 0; i < 4; i++) {
            Geometry fin = new Geometry("fin", new Box(0.05f * scale, 0.5f * scale, 0.45f * scale));
            fin.setMaterial(solid(new ColorRGBA(0.2f, 0.22f, 0.16f, 1f)));
            Node pivot = new Node("fin-pivot");
            fin.setLocalTranslation(0, 1.2f * scale, 0.6f * scale);
            pivot.attachChild(fin);
            pivot.setLocalRotation(new Quaternion().fromAngleAxis(i * FastMath.HALF_PI, Vector3f.UNIT_Y));
            shell.attachChild(pivot);
        }
        shell.setShadowMode(ShadowMode.Cast);
        return shell;
    }

    /**
     * Mörsergranate („Mörser 120mm“): Pfeifen, Einschlag, Explosion.
     *
     * @param target Einschlagpunkt
     * @return Tween bis zur Explosion
     */
    public Tween mortar(Vector3f target) {
        Node shell = shell(1f);
        Vector3f start = target.add(18, 85, -30);
        ParticleEmitter[] trail = new ParticleEmitter[1];
        return Tweens.seq(
                Tweens.call(() -> {
                    sounds.play(Sfx.WHISTLE, 1f, 0.05f);
                    shell.setLocalTranslation(start);
                    world.attachChild(shell);
                    trail[0] = particles.builder("fx/smoke.png", 40).atlas()
                                        .colors(new ColorRGBA(0.85f, 0.85f, 0.85f, 0.45f), CLEAR)
                                        .sizes(0.8f, 2.6f).life(0.6f, 1.1f).velocity(0, 1, 0, 1f).gravity(0, 0, 0)
                                        .rate(45).build();
                    trail[0].setLocalTranslation(0, 1.2f, 0);
                    shell.attachChild(trail[0]);
                }),
                Tweens.lerp(0.95f, Easing.IN_QUAD, t -> {
                    Vector3f p = new Vector3f().interpolateLocal(start, target.add(0, 0.5f, 0), t);
                    shell.setLocalTranslation(p);
                    Vector3f dir = target.subtract(start).normalizeLocal();
                    Quaternion q = new Quaternion();
                    q.lookAt(dir, Vector3f.UNIT_Y);
                    shell.setLocalRotation(q.mult(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X)));
                }),
                Tweens.call(() -> {
                    if (trail[0] != null) {
                        trail[0].removeFromParent();
                        trail[0].setLocalTranslation(shell.getWorldTranslation());
                    }
                    shell.removeFromParent();
                }),
                explosion(target, 1.5f));
    }

    /**
     * Fliegerbombe („GBU48“): fällt in die Tischmitte und zerstört alles.
     *
     * @param target Einschlagpunkt
     * @return Tween bis kurz nach der Explosion
     */
    public Tween bomb(Vector3f target) {
        Node bomb = shell(2.6f);
        Vector3f start = target.add(0, 120, -25);
        return Tweens.seq(
                Tweens.call(() -> {
                    sounds.play(Sfx.WHISTLE, 1f, 0f);
                    bomb.setLocalTranslation(start);
                    world.attachChild(bomb);
                    rig.focus(target, 175f, 58f);
                }),
                Tweens.par(
                        Tweens.lerp(1.25f, Easing.IN_CUBIC, t -> {
                            bomb.setLocalTranslation(new Vector3f().interpolateLocal(start, target.add(0, 3, 0), t));
                            bomb.setLocalRotation(new Quaternion().fromAngles(-0.2f * (1 - t), t * 3f, 0));
                        }),
                        shadowGrowth(target, 1.25f)),
                Tweens.call(bomb::removeFromParent),
                Tweens.par(explosion(target, 2.8f), screenFlash(ColorRGBA.White, 0.85f, 0.7f),
                           shockwave(target, 95f, new ColorRGBA(1f, 0.85f, 0.6f, 0.8f), 1.1f)),
                Tweens.call(() -> {
                    for (int i = 0; i < 5; i++) {
                        float a = i * FastMath.TWO_PI / 5;
                        smoke(target.add(FastMath.cos(a) * 18, 1, FastMath.sin(a) * 12), 9f, true);
                    }
                }),
                Tweens.delay(0.3f),
                Tweens.call(rig::release));
    }

    private Tween shadowGrowth(Vector3f target, float seconds) {
        Geometry shadow = flat("fx/glow.png", 1f, new ColorRGBA(0, 0, 0, 0), false);
        shadow.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        Node holder = new Node("bomb-shadow");
        shadow.setLocalTranslation(-0.5f, 0, 0.5f);
        holder.attachChild(shadow);
        holder.setLocalTranslation(target.x, 0.25f, target.z);
        return Tweens.seq(Tweens.call(() -> world.attachChild(holder)),
                          Tweens.lerp(seconds, Easing.IN_QUAD, t -> {
                              float s = 4 + t * 26;
                              holder.setLocalScale(s, 1, s);
                              shadow.getMaterial().setColor("Color", new ColorRGBA(0, 0, 0, 0.7f * t));
                          }),
                          Tweens.call(holder::removeFromParent));
    }

    /**
     * Hubschrauberangriff („50 Freedoms / Second“): Cockpit-Einblendung,
     * Leuchtspurmunition und Einschläge auf allen Zielen.
     *
     * @param targets Einschlagpunkte (Helden des Ziels)
     * @param focus   Punkt, auf den die Kamera schwenkt
     * @return Tween bis zum Ende des Beschusses
     */
    public Tween heliStrike(List<Vector3f> targets, Vector3f focus) {
        Node cockpit = new Node("heli");
        float w = cam.getWidth();
        float h = cam.getHeight();
        float imgW = Math.max(w, h * 16f / 9f);
        float imgH = imgW * 9f / 16f;
        Geometry frame = new Decoration("heli-frame", new Quad(imgW, imgH));
        Material fm = unshaded("images/heli.png", ColorRGBA.White, false);
        fm.getAdditionalRenderState().setDepthWrite(true);
        frame.setMaterial(fm);
        frame.setLocalTranslation((w - imgW) / 2, (h - imgH) / 2, 0);
        cockpit.attachChild(frame);
        Node muzzle = new Node("muzzle");
        Geometry flashQuad = new Decoration("muzzle-quad", new Quad(220, 220));
        flashQuad.setMaterial(unshaded("fx/muzzle.png", new ColorRGBA(1f, 0.85f, 0.45f, 1f), true));
        flashQuad.setLocalTranslation(-110, -110, 1);
        muzzle.attachChild(flashQuad);
        muzzle.setLocalTranslation(w * 0.5f + imgW * 0.005f, (h - imgH) / 2 + imgH * 0.33f, 1);
        muzzle.setCullHint(Spatial.CullHint.Always);
        cockpit.attachChild(muzzle);
        cockpit.setLocalTranslation(0, h, 60);

        List<Tween> volleys = new ArrayList<>();
        for (Vector3f target : targets) {
            volleys.add(Tweens.seq(Tweens.call(() -> burstFire(target)), Tweens.delay(0.55f)));
        }
        if (targets.isEmpty()) volleys.add(Tweens.call(() -> burstFire(focus)));
        return Tweens.seq(
                Tweens.call(() -> {
                    overlay.attachChild(cockpit);
                    sounds.play(Sfx.HELI);
                    rig.focus(focus, 95f, 34f);
                }),
                Tweens.lerp(0.9f, Easing.OUT_CUBIC, t -> cockpit.setLocalTranslation(0, h * (1 - t), 60)),
                Tweens.call(() -> {
                    sounds.play(Sfx.MINIGUN);
                    muzzle.setCullHint(Spatial.CullHint.Inherit);
                }),
                Tweens.par(Tweens.seq(volleys),
                           Tweens.lerp(0.55f * Math.max(1, targets.size()), t -> {
                               float jitter = 6f;
                               cockpit.setLocalTranslation((random.nextFloat() - 0.5f) * jitter,
                                                           (random.nextFloat() - 0.5f) * jitter, 60);
                               muzzle.setLocalScale(0.6f + random.nextFloat() * 0.7f);
                               muzzle.setLocalRotation(new Quaternion().fromAngleAxis(random.nextFloat() * 6,
                                                                                      Vector3f.UNIT_Z));
                           })),
                Tweens.call(() -> muzzle.setCullHint(Spatial.CullHint.Always)),
                Tweens.delay(0.3f),
                Tweens.lerp(0.7f, Easing.IN_CUBIC, t -> cockpit.setLocalTranslation(0, h * t, 60)),
                Tweens.call(() -> {
                    cockpit.removeFromParent();
                    rig.release();
                }));
    }

    private void burstFire(Vector3f target) {
        for (int i = 0; i < 12; i++) {
            float delay = i * 0.04f;
            Vector3f hit = target.add((random.nextFloat() - 0.5f) * 5, 0.3f, (random.nextFloat() - 0.5f) * 7);
            animator.play(Tweens.seq(Tweens.delay(delay), tracer(hit)));
        }
        animator.play(Tweens.seq(Tweens.delay(0.2f), Tweens.call(() -> {
            sparks(target.add(0, 1, 0), new ColorRGBA(1f, 0.8f, 0.35f, 1f), 50, 16f);
            smoke(target.add(0, 1, 0), 5f, true);
            rig.shake(0.7f, 0.25f);
        })));
    }

    private Tween tracer(Vector3f hit) {
        Vector3f camPos = cam.getLocation();
        Vector3f start = camPos.add(cam.getDirection().mult(12)).addLocal(cam.getUp().mult(-5));
        Geometry g = new Decoration("tracer", new Quad(0.9f, 11f));
        g.setMaterial(unshaded("fx/tracer.png", new ColorRGBA(1f, 0.9f, 0.5f, 1f), true));
        g.setQueueBucket(Bucket.Transparent);
        g.setLocalTranslation(-0.45f, -11f, 0);
        Node node = new Node("tracer");
        node.attachChild(g);
        Vector3f dir = hit.subtract(start).normalizeLocal();
        Quaternion q = new Quaternion();
        q.lookAt(camPos.subtract(start).normalizeLocal(), dir);
        node.setLocalRotation(q);
        return Tweens.seq(Tweens.call(() -> world.attachChild(node)),
                          Tweens.lerp(0.18f, t -> node.setLocalTranslation(
                                  new Vector3f().interpolateLocal(start, hit, t))),
                          Tweens.call(() -> {
                              node.removeFromParent();
                              emit(particles.builder("fx/spark.png", 10)
                                            .colors(new ColorRGBA(1f, 0.8f, 0.4f, 1f), CLEAR).sizes(1.6f, 0.2f)
                                            .life(0.25f, 0.5f).velocity(0, 8, 0, 1f).gravity(0, 14, 0)
                                            .additive().build(), hit);
                              animator.play(flash(hit.add(0, 1, 0), 5f, new ColorRGBA(1f, 0.8f, 0.4f, 0.9f), 0.2f));
                          }));
    }

    // ------------------------------------------------------------------
    // Bildschirm-Effekte
    // ------------------------------------------------------------------

    /**
     * Lässt den ganzen Bildschirm kurz aufleuchten.
     *
     * @param color   Farbe
     * @param alpha   Stärke
     * @param seconds Dauer
     * @return Tween
     */
    public Tween screenFlash(ColorRGBA color, float alpha, float seconds) {
        Geometry g = new Decoration("screen-flash", new Quad(cam.getWidth(), cam.getHeight()));
        Material m = unshaded(null, color, false);
        g.setMaterial(m);
        g.setLocalTranslation(0, 0, 80);
        return Tweens.seq(Tweens.call(() -> overlay.attachChild(g)),
                          Tweens.lerp(seconds, Easing.OUT_QUAD, t -> {
                              ColorRGBA c = color.clone();
                              c.a = alpha * (1 - t);
                              m.setColor("Color", c);
                          }),
                          Tweens.call(g::removeFromParent));
    }

    /**
     * Text, der über einem Punkt der Szene aufsteigt und verblasst.
     *
     * @param pos   Weltposition
     * @param text  Text
     * @param color Farbe
     * @param size  Schriftgröße in Referenzpixeln
     * @return Tween
     */
    public Tween floatingText(Vector3f pos, String text, ColorRGBA color, float size) {
        BitmapFont font = theme.headingFont();
        BitmapText label = new BitmapText(font);
        label.setText(text);
        label.setSize(size * UiScale.of(cam));
        label.setColor(color.clone());
        return Tweens.seq(
                Tweens.call(() -> overlay.attachChild(label)),
                Tweens.lerp(1.4f, Easing.OUT_CUBIC, t -> {
                    Vector3f screen = cam.getScreenCoordinates(pos);
                    float pop = t < 0.15f ? Easing.OUT_BACK.apply(t / 0.15f) : 1f;
                    label.setLocalScale(Math.max(0.01f, pop));
                    label.setLocalTranslation(screen.x - label.getLineWidth() * pop / 2,
                                              screen.y + 40 * UiScale.of(cam) + t * 70 * UiScale.of(cam)
                                              + label.getLineHeight() * pop / 2, 70);
                    ColorRGBA c = color.clone();
                    c.a = t > 0.7f ? 1 - (t - 0.7f) / 0.3f : 1f;
                    label.setColor(c);
                    label.setAlpha(c.a);
                }),
                Tweens.call(label::removeFromParent));
    }

    /**
     * Konfettiregen (Sieg).
     *
     * @param seconds Dauer
     * @return Tween
     */
    public Tween confetti(float seconds) {
        int count = 220;
        List<Piece> pieces = new ArrayList<>();
        Material[] mats = new Material[6];
        ColorRGBA[] colors = {Theme.GOLD, Theme.RED, Theme.GREEN, Theme.BLUE, Theme.PURPLE, Theme.CREAM};
        for (int i = 0; i < mats.length; i++) mats[i] = unshaded("fx/confetti.png", colors[i], false);
        Node node = new Node("confetti");
        node.setLocalTranslation(0, 0, 75);
        float w = cam.getWidth();
        float h = cam.getHeight();
        for (int i = 0; i < count; i++) {
            Piece p = new Piece();
            float size = (22 + random.nextFloat() * 18) * UiScale.of(cam);
            Geometry g = new Decoration("confetti", atlasQuad(size, random.nextInt(4)));
            g.setMaterial(mats[random.nextInt(mats.length)]);
            p.node = new Node("piece");
            p.node.attachChild(g);
            g.setLocalTranslation(-size / 2, -size / 2, 0);
            p.x = random.nextFloat() * w;
            p.y = h + random.nextFloat() * h * 0.6f;
            p.vx = (random.nextFloat() - 0.5f) * 120;
            p.vy = -(120 + random.nextFloat() * 220);
            p.spin = (random.nextFloat() - 0.5f) * 10;
            p.phase = random.nextFloat() * 6;
            pieces.add(p);
            node.attachChild(p.node);
        }
        float[] time = {0};
        return Tweens.seq(Tweens.call(() -> overlay.attachChild(node)),
                          Tweens.lerp(seconds, t -> {
                              float dt = t * seconds - time[0];
                              time[0] = t * seconds;
                              for (Piece p : pieces) {
                                  p.x += (p.vx + FastMath.sin(time[0] * 3 + p.phase) * 60) * dt;
                                  p.y += p.vy * dt;
                                  p.rot += p.spin * dt;
                                  p.node.setLocalTranslation(p.x, p.y, 0);
                                  p.node.setLocalRotation(new Quaternion().fromAngles(0, FastMath.sin(time[0] * 4 + p.phase), p.rot));
                              }
                          }),
                          Tweens.call(node::removeFromParent));
    }

    private static Mesh atlasQuad(float size, int cell) {
        Quad q = new Quad(size, size);
        float u0 = (cell % 2) * 0.5f;
        float v0 = (cell / 2) * 0.5f;
        q.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(u0, v0, u0 + 0.5f, v0, u0 + 0.5f, v0 + 0.5f,
                                                                     u0, v0 + 0.5f));
        return q;
    }

    /**
     * Ein Konfettistück.
     */
    private static final class Piece {
        private Node node;
        private float x;
        private float y;
        private float vx;
        private float vy;
        private float rot;
        private float spin;
        private float phase;
    }
}
