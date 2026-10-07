package pp.winf2slay.view.game.card;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Erzeugt die Geometrie der Karten: abgerundete Rechtecke für Vorder- und
 * Rückseite sowie den schmalen Rand.
 *
 * <p>Die Karte liegt in der xz-Ebene, die Vorderseite zeigt nach +y. Der obere
 * Bildrand liegt bei −z (vom Spieler weg).</p>
 */
public final class CardMeshes {

    private static final int CORNER_SEGMENTS = 6;
    private static final float CORNER_RADIUS = 0.42f;

    private static final Map<CardSize, Mesh> FRONT = new EnumMap<>(CardSize.class);
    private static final Map<CardSize, Mesh> BACK = new EnumMap<>(CardSize.class);
    private static final Map<CardSize, Mesh> EDGE = new EnumMap<>(CardSize.class);

    private CardMeshes() {
        // Utility-Klasse
    }

    /**
     * @param size Format
     * @return Mesh der Vorderseite (gemeinsam genutzt)
     */
    public static synchronized Mesh front(CardSize size) {
        return FRONT.computeIfAbsent(size, s -> face(s, true));
    }

    /**
     * @param size Format
     * @return Mesh der Rückseite (gemeinsam genutzt)
     */
    public static synchronized Mesh back(CardSize size) {
        return BACK.computeIfAbsent(size, s -> face(s, false));
    }

    /**
     * @param size Format
     * @return Mesh des Randes (gemeinsam genutzt)
     */
    public static synchronized Mesh edge(CardSize size) {
        return EDGE.computeIfAbsent(size, CardMeshes::edgeMesh);
    }

    /**
     * Umriss gegen den Uhrzeigersinn (von oben betrachtet).
     */
    private static List<Vector2f> outline(CardSize size) {
        float hw = size.getWidth() / 2;
        float hh = size.getHeight() / 2;
        float r = CORNER_RADIUS;
        float[][] corners = {{hw - r, -(hh - r), 0}, {-(hw - r), -(hh - r), 90},
                             {-(hw - r), hh - r, 180}, {hw - r, hh - r, 270}};
        List<Vector2f> points = new ArrayList<>();
        for (float[] c : corners) {
            for (int i = 0; i <= CORNER_SEGMENTS; i++) {
                float angle = (c[2] + 90f * i / CORNER_SEGMENTS) * FastMath.DEG_TO_RAD;
                points.add(new Vector2f(c[0] + r * FastMath.cos(angle), c[1] - r * FastMath.sin(angle)));
            }
        }
        return points;
    }

    private static Mesh face(CardSize size, boolean front) {
        List<Vector2f> outline = outline(size);
        int n = outline.size();
        float y = (front ? 1 : -1) * CardSize.THICKNESS / 2;
        float[] pos = new float[(n + 1) * 3];
        float[] nor = new float[(n + 1) * 3];
        float[] uv = new float[(n + 1) * 2];
        float w = size.getWidth();
        float h = size.getHeight();
        for (int i = 0; i <= n; i++) {
            Vector2f p = i < n ? outline.get(i) : new Vector2f(0, 0); // letzter Punkt = Mitte
            pos[i * 3] = p.x;
            pos[i * 3 + 1] = y;
            pos[i * 3 + 2] = p.y;
            nor[i * 3 + 1] = front ? 1 : -1;
            float u = (p.x + w / 2) / w;
            uv[i * 2] = front ? u : 1 - u;
            uv[i * 2 + 1] = (-p.y + h / 2) / h;
        }
        short[] idx = new short[n * 3];
        for (int i = 0; i < n; i++) {
            int a = i;
            int b = (i + 1) % n;
            idx[i * 3] = (short) n;
            idx[i * 3 + 1] = (short) (front ? a : b);
            idx[i * 3 + 2] = (short) (front ? b : a);
        }
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(pos));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(nor));
        mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(uv));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createShortBuffer(idx));
        mesh.updateBound();
        mesh.setStatic();
        return mesh;
    }

    private static Mesh edgeMesh(CardSize size) {
        List<Vector2f> outline = outline(size);
        int n = outline.size();
        float top = CardSize.THICKNESS / 2;
        float[] pos = new float[n * 2 * 3];
        float[] nor = new float[n * 2 * 3];
        float[] uv = new float[n * 2 * 2];
        for (int i = 0; i < n; i++) {
            Vector2f p = outline.get(i);
            Vector2f normal = p.subtract(nearestCenter(p, size)).normalizeLocal();
            for (int k = 0; k < 2; k++) {
                int v = i * 2 + k;
                pos[v * 3] = p.x;
                pos[v * 3 + 1] = k == 0 ? top : -top;
                pos[v * 3 + 2] = p.y;
                nor[v * 3] = normal.x;
                nor[v * 3 + 2] = normal.y;
                uv[v * 2] = (float) i / n;
                uv[v * 2 + 1] = k;
            }
        }
        short[] idx = new short[n * 6];
        for (int i = 0; i < n; i++) {
            int a = i * 2;
            int b = ((i + 1) % n) * 2;
            // Außenseite gegen den Uhrzeigersinn
            idx[i * 6] = (short) a;
            idx[i * 6 + 1] = (short) (a + 1);
            idx[i * 6 + 2] = (short) b;
            idx[i * 6 + 3] = (short) b;
            idx[i * 6 + 4] = (short) (a + 1);
            idx[i * 6 + 5] = (short) (b + 1);
        }
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(pos));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(nor));
        mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(uv));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createShortBuffer(idx));
        mesh.updateBound();
        mesh.setStatic();
        return mesh;
    }

    /**
     * Erzeugt eine abgerundete Kartenfläche in der xy-Ebene (für die Oberfläche).
     * Der Ursprung liegt in der Mitte, die Fläche zeigt nach +z.
     *
     * @param width  Breite in Pixeln
     * @param height Höhe in Pixeln
     * @param radius Eckradius in Pixeln
     * @return Mesh
     */
    public static Mesh flat(float width, float height, float radius) {
        float hw = width / 2;
        float hh = height / 2;
        float[][] corners = {{hw - radius, hh - radius, 0}, {-(hw - radius), hh - radius, 90},
                             {-(hw - radius), -(hh - radius), 180}, {hw - radius, -(hh - radius), 270}};
        List<Vector2f> outline = new ArrayList<>();
        for (float[] c : corners) {
            for (int i = 0; i <= CORNER_SEGMENTS; i++) {
                float angle = (c[2] + 90f * i / CORNER_SEGMENTS) * FastMath.DEG_TO_RAD;
                outline.add(new Vector2f(c[0] + radius * FastMath.cos(angle), c[1] + radius * FastMath.sin(angle)));
            }
        }
        int n = outline.size();
        float[] pos = new float[(n + 1) * 3];
        float[] uv = new float[(n + 1) * 2];
        float[] nor = new float[(n + 1) * 3];
        for (int i = 0; i <= n; i++) {
            Vector2f p = i < n ? outline.get(i) : new Vector2f();
            pos[i * 3] = p.x;
            pos[i * 3 + 1] = p.y;
            nor[i * 3 + 2] = 1;
            uv[i * 2] = (p.x + hw) / width;
            uv[i * 2 + 1] = (p.y + hh) / height;
        }
        short[] idx = new short[n * 3];
        for (int i = 0; i < n; i++) {
            idx[i * 3] = (short) n;
            idx[i * 3 + 1] = (short) i;
            idx[i * 3 + 2] = (short) ((i + 1) % n);
        }
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(pos));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(nor));
        mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(uv));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createShortBuffer(idx));
        mesh.updateBound();
        return mesh;
    }

    /**
     * Mittelpunkt der nächstgelegenen Eckrundung (für Randnormalen).
     */
    private static Vector2f nearestCenter(Vector2f p, CardSize size) {
        float cx = FastMath.clamp(p.x, -(size.getWidth() / 2 - CORNER_RADIUS), size.getWidth() / 2 - CORNER_RADIUS);
        float cz = FastMath.clamp(p.y, -(size.getHeight() / 2 - CORNER_RADIUS), size.getHeight() / 2 - CORNER_RADIUS);
        return new Vector2f(cx, cz);
    }
}
