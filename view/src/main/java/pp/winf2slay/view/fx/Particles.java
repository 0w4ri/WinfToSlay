package pp.winf2slay.view.fx;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;

/**
 * Baukasten für Partikelemitter (Funken, Rauch, Feuer, Trümmer …).
 *
 * <pre>{@code
 * ParticleEmitter sparks = particles.builder("fx/spark.png", 40)
 *     .colors(gold, transparent).sizes(0.6f, 0.1f).life(0.4f, 0.9f)
 *     .velocity(0, 12, 0, 1f).gravity(0, 20, 0).additive().build();
 * }</pre>
 */
public class Particles {

    private final AssetManager assets;
    private float density = 1f;

    /**
     * @param assets Asset-Manager
     */
    public Particles(AssetManager assets) {
        this.assets = assets;
    }

    /**
     * @param density Faktor für die Partikelanzahl (Grafikqualität)
     */
    public void setDensity(float density) {
        this.density = Math.max(0.2f, density);
    }

    /**
     * @param texture Textur (Einzelbild oder 2×2-Atlas, siehe {@link Builder#atlas()})
     * @param count   Höchstzahl der Partikel
     * @return Builder
     */
    public Builder builder(String texture, int count) {
        return new Builder(texture, Math.max(1, Math.round(count * density)));
    }

    /**
     * Fluent Builder für einen Emitter.
     */
    public final class Builder {
        private final ParticleEmitter emitter;
        private final Material material;

        private Builder(String texture, int count) {
            emitter = new ParticleEmitter("particles", ParticleMesh.Type.Triangle, count);
            material = new Material(assets, "Common/MatDefs/Misc/Particle.j3md");
            material.setTexture("Texture", assets.loadTexture(texture));
            material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            emitter.setMaterial(material);
            emitter.setQueueBucket(Bucket.Transparent);
            emitter.setShadowMode(ShadowMode.Off);
            emitter.setParticlesPerSec(0);
            emitter.setRandomAngle(true);
            emitter.setInWorldSpace(true);
        }

        /**
         * Textur ist ein 2×2-Atlas; jedes Partikel nimmt ein zufälliges Bild.
         *
         * @return dieser Builder
         */
        public Builder atlas() {
            emitter.setImagesX(2);
            emitter.setImagesY(2);
            emitter.setSelectRandomImage(true);
            return this;
        }

        /**
         * @param start Startfarbe
         * @param end   Endfarbe
         * @return dieser Builder
         */
        public Builder colors(ColorRGBA start, ColorRGBA end) {
            emitter.setStartColor(start);
            emitter.setEndColor(end);
            return this;
        }

        /**
         * @param start Startgröße
         * @param end   Endgröße
         * @return dieser Builder
         */
        public Builder sizes(float start, float end) {
            emitter.setStartSize(start);
            emitter.setEndSize(end);
            return this;
        }

        /**
         * @param low  kürzeste Lebensdauer
         * @param high längste Lebensdauer
         * @return dieser Builder
         */
        public Builder life(float low, float high) {
            emitter.setLowLife(low);
            emitter.setHighLife(high);
            return this;
        }

        /**
         * @param x         Startgeschwindigkeit x
         * @param y         Startgeschwindigkeit y
         * @param z         Startgeschwindigkeit z
         * @param variation Streuung 0..1
         * @return dieser Builder
         */
        public Builder velocity(float x, float y, float z, float variation) {
            emitter.getParticleInfluencer().setInitialVelocity(new Vector3f(x, y, z));
            emitter.getParticleInfluencer().setVelocityVariation(variation);
            return this;
        }

        /**
         * @param x Schwerkraft x (positiv = nach unten im jME-Sinn: Partikel fallen bei +y)
         * @param y Schwerkraft y
         * @param z Schwerkraft z
         * @return dieser Builder
         */
        public Builder gravity(float x, float y, float z) {
            emitter.setGravity(x, y, z);
            return this;
        }

        /**
         * @param radius Radius der Startkugel
         * @return dieser Builder
         */
        public Builder sphere(float radius) {
            emitter.setShape(new EmitterSphereShape(Vector3f.ZERO, radius));
            return this;
        }

        /**
         * @param speed Drehgeschwindigkeit der Partikel
         * @return dieser Builder
         */
        public Builder spin(float speed) {
            emitter.setRotateSpeed(speed);
            return this;
        }

        /**
         * Additives Mischen (leuchtende Effekte).
         *
         * @return dieser Builder
         */
        public Builder additive() {
            material.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
            return this;
        }

        /**
         * @param perSecond kontinuierliche Emission (0 = nur Ausstoß)
         * @return dieser Builder
         */
        public Builder rate(float perSecond) {
            emitter.setParticlesPerSec(perSecond);
            return this;
        }

        /**
         * @param facing Partikel richten sich nach der Bewegung aus (Funkenstreifen)
         * @return dieser Builder
         */
        public Builder facingVelocity(boolean facing) {
            emitter.setFacingVelocity(facing);
            return this;
        }

        /**
         * @return fertiger Emitter
         */
        public ParticleEmitter build() {
            return emitter;
        }
    }
}
