package pp.winf2slay.view.game;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * Übergang zwischen Bildschirm und 3D-Szene: Damit eine Karte aus der Hand
 * nahtlos auf den Tisch fliegt, wird sie als 3D-Karte genau dort vor die Kamera
 * gesetzt, wo sie auf dem Bildschirm zu sehen war – gleich groß und zur Kamera
 * gedreht.
 */
public class ScreenBridge {

    private final Camera cam;

    /**
     * @param cam Kamera
     */
    public ScreenBridge(Camera cam) {
        this.cam = cam;
    }

    /**
     * Lage einer 3D-Karte, die auf dem Bildschirm an {@code screen} mit der Höhe
     * {@code pixelHeight} erscheint.
     *
     * @param screen      Bildschirmposition der Kartenmitte (Pixel)
     * @param pixelHeight sichtbare Kartenhöhe in Pixeln
     * @param worldHeight Kartenhöhe in Weltkoordinaten
     * @param roll        Drehung in der Bildebene (Bogenmaß)
     * @return Lage
     */
    public Transform facingCamera(Vector3f screen, float pixelHeight, float worldHeight, float roll) {
        float fovY = 2 * FastMath.atan(1f / cam.getProjectionMatrix().m11);
        float distance = worldHeight * cam.getHeight() / (2 * FastMath.tan(fovY / 2) * Math.max(1, pixelHeight));
        Vector3f near = cam.getWorldCoordinates(new Vector2f(screen.x, screen.y), 0f);
        Vector3f dir = cam.getWorldCoordinates(new Vector2f(screen.x, screen.y), 1f).subtractLocal(near)
                          .normalizeLocal();
        float along = distance / Math.max(0.2f, dir.dot(cam.getDirection()));
        Vector3f pos = cam.getLocation().add(dir.mult(along));
        Vector3f right = cam.getLeft().negate();
        Vector3f up = cam.getUp();
        Vector3f back = cam.getDirection().negate();
        Quaternion q = new Quaternion().fromAxes(right, back, up.negate());
        q = q.mult(new Quaternion().fromAngleAxis(roll, Vector3f.UNIT_Y));
        return new Transform(pos, q);
    }

    /**
     * @return Fensterbreite in Pixeln
     */
    public float screenWidth() {
        return cam.getWidth();
    }

    /**
     * @param world Weltposition
     * @return Bildschirmposition (Pixel)
     */
    public Vector3f screen(Vector3f world) {
        return cam.getScreenCoordinates(world);
    }
}
