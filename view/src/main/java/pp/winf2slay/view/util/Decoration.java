package pp.winf2slay.view.util;

import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;

/**
 * Geometrie, die bei Mausabfragen ignoriert wird (Schatten, Leuchtrahmen, Effekte).
 * So „stehlen“ Zierelemente den eigentlichen Karten keine Klicks.
 */
public class Decoration extends Geometry {

    /**
     * Konstruktor für die Serialisierung von jME.
     */
    public Decoration() {
        super();
    }

    /**
     * @param name Name
     * @param mesh Mesh
     */
    public Decoration(String name, Mesh mesh) {
        super(name, mesh);
    }

    @Override
    public int collideWith(Collidable other, CollisionResults results) {
        return 0;
    }
}
