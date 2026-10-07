package pp.winf2slay.view.game;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import pp.winf2slay.view.settings.UserSettings.Quality;
import pp.winf2slay.view.util.Decoration;

import java.util.Random;

/**
 * Die 3D-Umgebung: Taverne (Skybox), Spieltisch mit Filzmatte, Kerzen, Licht
 * und Nachbearbeitung (Schatten, Leuchten, Kantenglättung).
 *
 * <p>Die Szene wird einmal beim Start aufgebaut und von Menü und Spiel
 * gemeinsam genutzt – so kann die Kamera vom Menü direkt an den Tisch fliegen.</p>
 */
public class TableScene {

    /** Halbe Tischbreite (x). */
    public static final float HALF_WIDTH = 100f;
    /** Halbe Tischtiefe (z). */
    public static final float HALF_DEPTH = 68f;

    private final SimpleApplication app;
    private final AssetManager assets;
    private final Node root = new Node("table-scene");
    private final Node board = new Node("board");
    private final Node effects = new Node("effects");
    private final Node decor = new Node("menu-decor");
    private final DirectionalLight sun = new DirectionalLight();
    private final AmbientLight ambient = new AmbientLight();
    private FilterPostProcessor fpp;
    private Quality quality;

    /**
     * @param app Anwendung
     */
    public TableScene(SimpleApplication app) {
        this.app = app;
        this.assets = app.getAssetManager();
    }

    /**
     * Baut die Szene auf und hängt sie in den Szenengraphen.
     *
     * @param quality Grafikqualität
     */
    public void build(Quality quality) {
        app.getRenderManager().setPreferredLightMode(TechniqueDef.LightMode.SinglePass);
        app.getRenderManager().setSinglePassLightBatchSize(4);
        root.setShadowMode(ShadowMode.Off);
        root.attachChild(createSky());
        root.attachChild(createTable());
        root.attachChild(board);
        root.attachChild(effects);
        root.attachChild(decor);
        createLights();
        createCandle(new Vector3f(-HALF_WIDTH + 12, 0, -HALF_DEPTH + 12));
        createCandle(new Vector3f(HALF_WIDTH - 12, 0, -HALF_DEPTH + 12));
        app.getRootNode().attachChild(root);
        applyQuality(quality);
    }

    /**
     * @return Knoten für Karten und Spielfeldelemente
     */
    public Node getBoard() {
        return board;
    }

    /**
     * @return Knoten für die Dekoration im Hauptmenü (unabhängig vom Spielfeld,
     * das beim Verlassen eines Spiels geleert wird)
     */
    public Node getDecor() {
        return decor;
    }

    /**
     * @return Knoten für Effekte (Partikel, Blitze …)
     */
    public Node getEffects() {
        return effects;
    }

    /**
     * Schaltet Schatten und Nachbearbeitung je nach Qualität.
     *
     * @param newQuality Grafikqualität
     */
    public void applyQuality(Quality newQuality) {
        if (newQuality == quality) return;
        quality = newQuality;
        ViewPort vp = app.getViewPort();
        if (fpp != null) {
            vp.removeProcessor(fpp);
            fpp = null;
        }
        if (quality == Quality.HIGH) {
            fpp = new FilterPostProcessor(assets);
            DirectionalLightShadowFilter shadows = new DirectionalLightShadowFilter(assets, 2048, 2);
            shadows.setLight(sun);
            shadows.setShadowIntensity(0.45f);
            shadows.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
            shadows.setShadowZExtend(320f);
            fpp.addFilter(shadows);
            BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
            bloom.setBloomIntensity(1.6f);
            bloom.setBlurScale(1.4f);
            fpp.addFilter(bloom);
            fpp.addFilter(new FXAAFilter());
            vp.addProcessor(fpp);
        }
    }

    /**
     * @return aktuelle Grafikqualität
     */
    public Quality getQuality() {
        return quality;
    }

    private Spatial createSky() {
        Texture west = assets.loadTexture("textures/sky/right.jpg");
        Texture east = assets.loadTexture("textures/sky/left.jpg");
        Texture north = assets.loadTexture("textures/sky/front.jpg");
        Texture south = assets.loadTexture("textures/sky/back.jpg");
        Texture up = assets.loadTexture("textures/sky/up.jpg");
        Texture down = assets.loadTexture("textures/sky/down.jpg");
        Spatial sky = SkyFactory.createSky(assets, west, east, north, south, up, down);
        sky.setName("sky");
        return sky;
    }

    private Node createTable() {
        Node table = new Node("table");
        Texture diffuse = loadRepeat("textures/table/wood_diff.jpg");
        Texture normal = loadRepeat("textures/table/wood_nor_dx.jpg");

        // Tischplatte
        Box topBox = new Box(HALF_WIDTH, 2f, HALF_DEPTH);
        topBox.scaleTextureCoordinates(new com.jme3.math.Vector2f(3f, 2f));
        Geometry top = new Geometry("table-top", topBox);
        top.setMaterial(wood(diffuse, normal, new ColorRGBA(0.82f, 0.7f, 0.6f, 1f)));
        top.setLocalTranslation(0, -2f, 0);
        top.setShadowMode(ShadowMode.Receive);
        MikktspaceTangentGenerator.generate(top);
        table.attachChild(top);

        // Erhöhter Holzrand
        Material rim = wood(diffuse, normal, new ColorRGBA(0.5f, 0.36f, 0.26f, 1f));
        float t = 4f;
        float h = 1.4f;
        addRim(table, rim, new Vector3f(0, h / 2 - 0.2f, -HALF_DEPTH + t / 2), HALF_WIDTH, h, t / 2);
        addRim(table, rim, new Vector3f(0, h / 2 - 0.2f, HALF_DEPTH - t / 2), HALF_WIDTH, h, t / 2);
        addRim(table, rim, new Vector3f(-HALF_WIDTH + t / 2, h / 2 - 0.2f, 0), t / 2, h, HALF_DEPTH - t);
        addRim(table, rim, new Vector3f(HALF_WIDTH - t / 2, h / 2 - 0.2f, 0), t / 2, h, HALF_DEPTH - t);

        // Beine (sichtbar beim Kameraflug im Menü)
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                Geometry leg = new Geometry("leg", new Box(3.5f, 40f, 3.5f));
                leg.setMaterial(rim);
                leg.setLocalTranslation(sx * (HALF_WIDTH - 10), -44f, sz * (HALF_DEPTH - 10));
                table.attachChild(leg);
            }
        }

        // Filzmatte in der Mitte
        Geometry felt = flatQuad("felt", 158f, 98f, 0.03f);
        Material feltMat = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        feltMat.setTexture("DiffuseMap", assets.loadTexture(new TextureKey("textures/table/felt.png", true)));
        feltMat.setBoolean("UseMaterialColors", true);
        feltMat.setColor("Diffuse", ColorRGBA.White.clone());
        feltMat.setColor("Ambient", ColorRGBA.White.clone());
        feltMat.setColor("Specular", ColorRGBA.Black.clone());
        feltMat.setFloat("AlphaDiscardThreshold", 0.5f);
        felt.setMaterial(feltMat);
        felt.setShadowMode(ShadowMode.Receive);
        table.attachChild(felt);
        return table;
    }

    private void addRim(Node parent, Material mat, Vector3f center, float hx, float hy, float hz) {
        Geometry g = new Geometry("rim", new Box(hx, hy / 2, hz));
        g.setMaterial(mat);
        g.setLocalTranslation(center);
        g.setShadowMode(ShadowMode.CastAndReceive);
        MikktspaceTangentGenerator.generate(g);
        parent.attachChild(g);
    }

    private Texture loadRepeat(String path) {
        TextureKey key = new TextureKey(path, true);
        key.setGenerateMips(true);
        Texture t = assets.loadTexture(key);
        t.setWrap(WrapMode.Repeat);
        t.setAnisotropicFilter(4);
        return t;
    }

    private Material wood(Texture diffuse, Texture normal, ColorRGBA tint) {
        Material m = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("DiffuseMap", diffuse);
        m.setTexture("NormalMap", normal);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", tint);
        m.setColor("Ambient", tint);
        m.setColor("Specular", new ColorRGBA(0.2f, 0.17f, 0.12f, 1f));
        m.setFloat("Shininess", 18f);
        return m;
    }

    /**
     * Erzeugt ein flaches Rechteck in der xz-Ebene (Mitte im Ursprung, Oberseite +y),
     * das bei Mausabfragen ignoriert wird.
     *
     * @param name   Name
     * @param width  Breite (x)
     * @param depth  Tiefe (z)
     * @param height Höhe über dem Tisch
     * @return Geometrie
     */
    public static Geometry flatQuad(String name, float width, float depth, float height) {
        Geometry g = new Decoration(name, new Quad(width, depth));
        g.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        g.setLocalTranslation(-width / 2, height, depth / 2);
        return g;
    }

    private void createLights() {
        sun.setDirection(new Vector3f(-0.35f, -1f, -0.55f).normalizeLocal());
        sun.setColor(new ColorRGBA(1f, 0.9f, 0.76f, 1f).multLocal(0.72f));
        ambient.setColor(new ColorRGBA(1f, 0.92f, 0.82f, 1f).multLocal(0.62f));
        app.getRootNode().addLight(sun);
        app.getRootNode().addLight(ambient);
    }

    private void createCandle(Vector3f base) {
        Node candle = new Node("candle");
        Material wax = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        wax.setBoolean("UseMaterialColors", true);
        wax.setColor("Diffuse", new ColorRGBA(0.95f, 0.88f, 0.72f, 1f));
        wax.setColor("Ambient", new ColorRGBA(0.95f, 0.88f, 0.72f, 1f));
        wax.setColor("GlowColor", new ColorRGBA(0.25f, 0.15f, 0.05f, 1f));
        Geometry body = new Geometry("wax", new Cylinder(8, 20, 1.6f, 9f, true));
        body.setMaterial(wax);
        body.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        body.setLocalTranslation(0, 4.5f, 0);
        body.setShadowMode(ShadowMode.Cast);
        candle.attachChild(body);

        Geometry plate = new Geometry("plate", new Cylinder(4, 24, 3.2f, 0.5f, true));
        Material brass = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        brass.setBoolean("UseMaterialColors", true);
        brass.setColor("Diffuse", new ColorRGBA(0.72f, 0.52f, 0.2f, 1f));
        brass.setColor("Ambient", new ColorRGBA(0.5f, 0.36f, 0.14f, 1f));
        brass.setColor("Specular", new ColorRGBA(1f, 0.85f, 0.5f, 1f));
        brass.setFloat("Shininess", 40f);
        plate.setMaterial(brass);
        plate.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        plate.setLocalTranslation(0, 0.25f, 0);
        candle.attachChild(plate);

        ParticleEmitter flame = new ParticleEmitter("flame", ParticleMesh.Type.Triangle, 24);
        Material fm = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
        fm.setTexture("Texture", assets.loadTexture("fx/glow.png"));
        flame.setMaterial(fm);
        flame.setStartColor(new ColorRGBA(1f, 0.85f, 0.4f, 0.9f));
        flame.setEndColor(new ColorRGBA(1f, 0.3f, 0.05f, 0f));
        flame.setStartSize(0.9f);
        flame.setEndSize(0.25f);
        flame.setGravity(0, -2.2f, 0);
        flame.setLowLife(0.35f);
        flame.setHighLife(0.6f);
        flame.setParticlesPerSec(30);
        flame.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 1.2f, 0));
        flame.getParticleInfluencer().setVelocityVariation(0.2f);
        flame.setLocalTranslation(0, 9.6f, 0);
        flame.setQueueBucket(Bucket.Transparent);
        candle.attachChild(flame);

        Geometry halo = new Geometry("halo", new Quad(7, 7));
        Material hm = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        hm.setTexture("ColorMap", assets.loadTexture("fx/glow.png"));
        hm.setColor("Color", new ColorRGBA(1f, 0.7f, 0.3f, 0.45f));
        hm.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
        hm.getAdditionalRenderState().setDepthWrite(false);
        halo.setMaterial(hm);
        halo.setQueueBucket(Bucket.Transparent);
        Node haloNode = new Node("halo-node");
        halo.setLocalTranslation(-3.5f, -3.5f, 0);
        haloNode.attachChild(halo);
        haloNode.addControl(new com.jme3.scene.control.BillboardControl());
        haloNode.setLocalTranslation(0, 10f, 0);
        candle.attachChild(haloNode);

        PointLight light = new PointLight(base.add(0, 13f, 0), new ColorRGBA(1f, 0.62f, 0.3f, 1f), 90f);
        app.getRootNode().addLight(light);
        candle.addControl(new FlickerControl(light, hm));
        candle.setLocalTranslation(base);
        root.attachChild(candle);
    }

    /**
     * Lässt Kerzenlicht und Lichthof flackern.
     */
    private static final class FlickerControl extends AbstractControl {
        private final PointLight light;
        private final Material halo;
        private final Random random = new Random();
        private final ColorRGBA base;
        private float time;
        private float level = 1f;

        private FlickerControl(PointLight light, Material halo) {
            this.light = light;
            this.halo = halo;
            this.base = light.getColor().clone();
        }

        @Override
        protected void controlUpdate(float tpf) {
            time += tpf;
            float target = 0.85f + 0.1f * FastMath.sin(time * 7.3f) + 0.08f * FastMath.sin(time * 13.1f)
                           + (random.nextFloat() - 0.5f) * 0.08f;
            level += (target - level) * Math.min(1, tpf * 12);
            light.setColor(base.mult(level * 0.55f));
            halo.setColor("Color", new ColorRGBA(1f, 0.7f, 0.3f, 0.38f * level));
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            // nichts zu tun
        }
    }
}
