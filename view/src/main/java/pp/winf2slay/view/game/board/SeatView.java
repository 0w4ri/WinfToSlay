package pp.winf2slay.view.game.board;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import pp.winf2slay.model.field.Group;
import pp.winf2slay.view.game.BoardLayout;
import pp.winf2slay.view.game.TableScene;
import pp.winf2slay.view.game.card.CardNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Darstellung eines Spielerplatzes: Ledermatte, Kartenplätze und die Karten des
 * Spielers (Anführer, Helden, Trophäen, ausgespielte Karte, verdeckte Hand).
 */
public class SeatView {

    private final String player;
    private final int seat;
    private final ColorRGBA color;
    private final Node decor = new Node("seat-decor");
    private final Geometry activeGlow;
    private final Material activeMat;
    private final List<Geometry> slotMarkers = new ArrayList<>();

    /** Anführer. */
    CardNode leader;
    /** Helden je Platz. */
    final CardNode[] heroes = new CardNode[Group.SIZE];
    /** Besiegte Monster. */
    final List<CardNode> trophies = new ArrayList<>();
    /** Gerade ausgespielte Karte. */
    CardNode hover;
    /** Verdeckte Handkarten (nur Gegner). */
    final List<CardNode> hand = new ArrayList<>();

    private boolean active;
    private float time;

    /**
     * @param assets Asset-Manager
     * @param layout Tischaufteilung
     * @param seat   Platznummer
     * @param player Spielername
     * @param color  Spielerfarbe
     */
    public SeatView(AssetManager assets, BoardLayout layout, int seat, String player, ColorRGBA color) {
        this.player = player;
        this.seat = seat;
        this.color = color;
        Transform place = layout.seat(seat);
        decor.setLocalTransform(place);

        Geometry mat = TableScene.flatQuad("seat-mat", 60f, 17f, 0.05f);
        mat.move(0, 0, -1.2f);
        Material matMaterial = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        matMaterial.setTexture("DiffuseMap", assets.loadTexture("textures/table/seat_mat.png"));
        matMaterial.setBoolean("UseMaterialColors", true);
        matMaterial.setColor("Diffuse", ColorRGBA.White.clone());
        matMaterial.setColor("Ambient", ColorRGBA.White.clone());
        matMaterial.setColor("Specular", ColorRGBA.Black.clone());
        matMaterial.setFloat("AlphaDiscardThreshold", 0.5f);
        mat.setMaterial(matMaterial);
        mat.setShadowMode(ShadowMode.Receive);
        decor.attachChild(mat);

        Material slotMat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        slotMat.setTexture("ColorMap", assets.loadTexture("fx/slot.png"));
        ColorRGBA slotColor = color.clone();
        slotColor.a = 0.55f;
        slotMat.setColor("Color", slotColor);
        slotMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        slotMat.getAdditionalRenderState().setDepthWrite(false);
        for (int i = 0; i < Group.SIZE; i++) {
            Geometry slot = TableScene.flatQuad("slot", 6.6f, 9.2f, 0.07f);
            slot.move((i - (Group.SIZE - 1) / 2f) * BoardLayout.HERO_SPACING, 0, -1.5f);
            slot.setMaterial(slotMat);
            slot.setQueueBucket(Bucket.Transparent);
            slotMarkers.add(slot);
            decor.attachChild(slot);
        }

        activeGlow = TableScene.flatQuad("seat-glow", 74f, 30f, 0.06f);
        activeGlow.move(0, 0, -1.2f);
        activeMat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        activeMat.setTexture("ColorMap", assets.loadTexture("fx/seat_glow.png"));
        activeMat.setColor("Color", color.clone());
        activeMat.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
        activeMat.getAdditionalRenderState().setDepthWrite(false);
        activeGlow.setMaterial(activeMat);
        activeGlow.setQueueBucket(Bucket.Transparent);
        activeGlow.setCullHint(Node.CullHint.Always);
        decor.attachChild(activeGlow);
    }

    /**
     * @return Spielername
     */
    public String getPlayer() {
        return player;
    }

    /**
     * @return Platznummer (0 = eigener Platz)
     */
    public int getSeat() {
        return seat;
    }

    /**
     * @return Spielerfarbe
     */
    public ColorRGBA getColor() {
        return color.clone();
    }

    /**
     * @return Knoten mit Matte und Markierungen
     */
    public Node getDecor() {
        return decor;
    }

    /**
     * @param active {@code true}, wenn der Spieler am Zug ist
     */
    public void setActive(boolean active) {
        this.active = active;
        activeGlow.setCullHint(active ? Node.CullHint.Inherit : Node.CullHint.Always);
    }

    /**
     * @return {@code true}, wenn der Spieler am Zug ist
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Lässt den Rahmen des aktiven Spielers pulsieren.
     *
     * @param tpf Zeit seit dem letzten Bild
     */
    public void update(float tpf) {
        time += tpf;
        if (active) {
            float pulse = 0.55f + 0.25f * FastMath.sin(time * 2.6f);
            activeMat.setColor("Color", color.mult(pulse));
        }
    }

    /**
     * @return Anzahl belegter Heldenplätze
     */
    public int heroCount() {
        int n = 0;
        for (CardNode h : heroes) if (h != null) n++;
        return n;
    }

    /**
     * @return alle Karten des Platzes (ohne verdeckte Hand)
     */
    public List<CardNode> visibleCards() {
        List<CardNode> list = new ArrayList<>();
        if (leader != null) list.add(leader);
        for (CardNode h : heroes) if (h != null) list.add(h);
        list.addAll(trophies);
        if (hover != null) list.add(hover);
        return list;
    }
}
