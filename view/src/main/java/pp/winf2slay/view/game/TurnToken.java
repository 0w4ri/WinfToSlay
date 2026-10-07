package pp.winf2slay.view.game;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.SkinningControl;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Cylinder;
import pp.winf2slay.view.anim.Easing;
import pp.winf2slay.view.anim.Tween;
import pp.winf2slay.view.anim.Tweens;
import pp.winf2slay.view.ui.Theme;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * Die Ritterfigur aus dem Gruppenprojekt als Zugmarker: Sie steht neben dem Platz
 * des Spielers, der am Zug ist, und hüpft beim Zugwechsel zum nächsten Platz.
 */
public class TurnToken {

    private static final Logger LOGGER = System.getLogger(TurnToken.class.getName());
    private static final float HEIGHT = 11f;
    /**
     * Blickziel der Figur: ungefähr die Kameraposition über dem eigenen Platz, damit
     * die Figur an jedem Platz dem Betrachter zugewandt ist.
     */
    private static final float BASE_RADIUS = 3.2f;
    private static final float BASE_HEIGHT = 0.7f;
    private static final Vector3f LOOK_AT = new Vector3f(0, 0, 80);

    private final BoardLayout layout;
    private final Node token = new Node("turn-token");
    private final Node body = new Node("turn-token-body");
    private final Material baseMaterial;
    private int seat = -1;

    /**
     * @param assets Asset-Manager
     * @param parent Knoten im Spielfeld
     * @param layout Tischaufteilung
     */
    public TurnToken(AssetManager assets, Node parent, BoardLayout layout) {
        this.layout = layout;
        // Sockel in der Farbe des Spielers am Zug
        baseMaterial = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        baseMaterial.setBoolean("UseMaterialColors", true);
        baseMaterial.setColor("Diffuse", ColorRGBA.White.clone());
        baseMaterial.setColor("Ambient", ColorRGBA.White.clone());
        baseMaterial.setColor("Specular", new ColorRGBA(0.6f, 0.6f, 0.6f, 1f));
        baseMaterial.setFloat("Shininess", 48f);
        Geometry base = new Geometry("turn-token-base", new Cylinder(2, 40, BASE_RADIUS, BASE_HEIGHT, true));
        base.setMaterial(baseMaterial);
        base.rotate(FastMath.HALF_PI, 0, 0);
        base.setLocalTranslation(0, BASE_HEIGHT / 2, 0);
        base.setShadowMode(ShadowMode.CastAndReceive);
        token.attachChild(base);
        body.setLocalTranslation(0, BASE_HEIGHT, 0);
        try {
            Spatial knight = assets.loadModel("models/knight.glb");
            replacePbrMaterials(assets, knight);
            bakePose(knight);
            knight.updateGeometricState();
            BoundingBox box = (BoundingBox) knight.getWorldBound();
            float height = box.getYExtent() * 2;
            float scale = HEIGHT / Math.max(0.01f, height);
            knight.setLocalScale(knight.getLocalScale().mult(scale));
            knight.updateGeometricState();
            box = (BoundingBox) knight.getWorldBound();
            Vector3f center = box.getCenter();
            knight.move(-center.x, -(center.y - box.getYExtent()), -center.z);
            knight.setShadowMode(ShadowMode.Cast);
            body.attachChild(knight);
        }
        catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Ritterfigur konnte nicht geladen werden: {0}", e.getMessage());
        }
        token.attachChild(body);
        token.addControl(new IdleControl());
        token.setCullHint(Spatial.CullHint.Always);
        parent.attachChild(token);
    }

    /**
     * glTF-Modelle nutzen PBR-Materialien, die ohne Lichtsonde dunkel wirken – sie
     * werden durch klassische Phong-Materialien mit derselben Farbtextur ersetzt.
     */
    private static void replacePbrMaterials(AssetManager assets, Spatial model) {
        model.depthFirstTraversal(s -> {
            if (!(s instanceof Geometry g)) return;
            Material old = g.getMaterial();
            Material m = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
            m.setBoolean("UseMaterialColors", true);
            m.setColor("Diffuse", ColorRGBA.White.clone());
            m.setColor("Ambient", ColorRGBA.White.clone());
            m.setColor("Specular", new ColorRGBA(0.3f, 0.3f, 0.3f, 1f));
            m.setFloat("Shininess", 20f);
            MatParamTexture base = old == null ? null : old.getTextureParam("BaseColorMap");
            if (base != null) m.setTexture("DiffuseMap", base.getTextureValue());
            g.setMaterial(m);
        });
    }

    /**
     * Das Modell ist mit einem Skelett versehen, aber nicht animiert. Die Pose wird
     * einmalig in die Vertices übernommen (Software-Skinning) und das Skelett danach
     * entfernt – so stimmen die Begrenzungsvolumen für Größe und Position, und pro
     * Bild fällt keine Skinning-Arbeit an.
     */
    private static void bakePose(Spatial model) {
        List<Spatial> skinned = new ArrayList<>();
        model.depthFirstTraversal(s -> {
            if (s.getControl(SkinningControl.class) != null) skinned.add(s);
        });
        for (Spatial s : skinned) {
            SkinningControl skinning = s.getControl(SkinningControl.class);
            skinning.setHardwareSkinningPreferred(false);
            s.updateLogicalState(0);
            skinning.render(null, null);
            s.removeControl(skinning);
            AnimComposer composer = s.getControl(AnimComposer.class);
            if (composer != null) s.removeControl(composer);
        }
        model.depthFirstTraversal(s -> {
            if (s instanceof Geometry g) g.updateModelBound();
        });
    }

    private Vector3f placeOf(int s) {
        return layout.seat(s).transformVector(new Vector3f(-31f, 0.1f, 3f), null);
    }

    private Quaternion facing(Vector3f pos) {
        float yaw = FastMath.atan2(LOOK_AT.x - pos.x, LOOK_AT.z - pos.z);
        return new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y);
    }

    /**
     * Zieht die Figur zu einem Platz.
     *
     * @param target Platz
     * @return Tween (Sprünge über den Tisch)
     */
    public Tween moveTo(int target) {
        return Tweens.defer(() -> {
            if (target < 0) return Tweens.NONE;
            Vector3f end = placeOf(target);
            baseMaterial.setColor("Diffuse", Theme.playerColor(target));
            if (seat < 0) {
                seat = target;
                token.setLocalTranslation(end);
                token.setLocalRotation(facing(end));
                token.setCullHint(Spatial.CullHint.Inherit);
                return Tweens.lerp(0.5f, Easing.OUT_BACK, t -> token.setLocalScale(Math.max(0.01f, t)));
            }
            if (seat == target) return Tweens.NONE;
            seat = target;
            Vector3f start = token.getLocalTranslation().clone();
            Quaternion from = token.getLocalRotation().clone();
            Quaternion to = facing(end);
            int hops = 3;
            return Tweens.lerp(1.1f, Easing.IN_OUT_CUBIC, t -> {
                Vector3f p = new Vector3f().interpolateLocal(start, end, t);
                float phase = t * hops;
                p.y += FastMath.abs(FastMath.sin(phase * FastMath.PI)) * 5f;
                token.setLocalTranslation(p);
                Quaternion q = new Quaternion();
                q.slerp(from, to, Math.min(1, t * 1.5f));
                token.setLocalRotation(q);
            });
        });
    }

    /**
     * Leichtes Wippen im Stand.
     */
    private final class IdleControl extends AbstractControl {
        private float time;

        @Override
        protected void controlUpdate(float tpf) {
            time += tpf;
            body.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.sin(time * 1.4f) * 0.12f, Vector3f.UNIT_Y));
            body.setLocalTranslation(0, BASE_HEIGHT + FastMath.abs(FastMath.sin(time * 2.2f)) * 0.25f, 0);
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            // nichts zu tun
        }
    }
}
