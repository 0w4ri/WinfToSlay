package pp.winf2slay.view.dev;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.system.Timer;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entwicklerwerkzeug ({@code -Dwinf.record=<Ordner>}): Das Spiel läuft mit fester
 * Bildrate, und jedes n-te Bild wird als JPEG gespeichert. So lassen sich
 * Animationen auch auf langsamen Rechnern Bild für Bild prüfen.
 */
public class FrameRecorder extends BaseAppState {

    private final File folder;
    private final int every;
    private final float fps;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WinfToSlay-Recorder");
        t.setDaemon(true);
        return t;
    });
    private Renderer renderer;
    private ByteBuffer buffer;
    private int width;
    private int height;
    private long frame;

    /**
     * @param folder Zielordner
     * @param every  jedes wievielte Bild gespeichert wird
     * @param fps    feste Bildrate der Spielzeit
     */
    public FrameRecorder(File folder, int every, float fps) {
        this.folder = folder;
        this.every = Math.max(1, every);
        this.fps = fps;
    }

    @Override
    protected void initialize(Application app) {
        folder.mkdirs();
        List<ViewPort> views = app.getRenderManager().getPostViews();
        views.getLast().addProcessor(new Grabber());
        app.setTimer(new FixedTimer(fps));
    }

    @Override
    protected void cleanup(Application app) {
        writer.shutdown();
    }

    @Override
    protected void onEnable() {
        // nichts zu tun
    }

    @Override
    protected void onDisable() {
        // nichts zu tun
    }

    private void write(byte[] data, int w, int h, float seconds) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            int row = (h - 1 - y) * w * 4;
            for (int x = 0; x < w; x++) {
                int i = row + x * 4;
                int r = data[i] & 0xff;
                int g = data[i + 1] & 0xff;
                int b = data[i + 2] & 0xff;
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        BufferedImage small = new BufferedImage(w / 2, h / 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w / 2, h / 2, null);
        g.dispose();
        try {
            ImageIO.write(small, "jpg", new File(folder, String.format(Locale.ROOT, "f_%06.2f.jpg", seconds)));
        }
        catch (IOException e) {
            System.getLogger(FrameRecorder.class.getName()).log(System.Logger.Level.WARNING, e.getMessage());
        }
    }

    /**
     * Liest nach jedem Bild den Bildpuffer aus.
     */
    private final class Grabber implements SceneProcessor {
        private boolean ready;

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        renderer = rm.getRenderer();
        reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
        ready = true;
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        width = w;
        height = h;
        buffer = BufferUtils.createByteBuffer(w * h * 4);
    }

    @Override
    public boolean isInitialized() {
        return ready;
    }

    @Override
    public void preFrame(float tpf) {
        // nichts zu tun
    }

    @Override
    public void postQueue(RenderQueue rq) {
        // nichts zu tun
    }

    @Override
    public void postFrame(FrameBuffer out) {
        if (frame++ % every != 0) return;
        buffer.clear();
        renderer.readFrameBuffer(out, buffer);
        byte[] data = new byte[width * height * 4];
        buffer.get(data);
        int w = width;
        int h = height;
        float seconds = frame / fps;
        writer.submit(() -> write(data, w, h, seconds));
    }

    @Override
    public void cleanup() {
        ready = false;
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
        // nicht benötigt
    }

    }

    /**
     * Zeitgeber mit fester Schrittweite – unabhängig von der echten Rechenzeit.
     */
    private static final class FixedTimer extends Timer {
        private final float fps;
        private long ticks;

        private FixedTimer(float fps) {
            this.fps = fps;
        }

        @Override
        public long getTime() {
            return ticks;
        }

        @Override
        public long getResolution() {
            return (long) fps;
        }

        @Override
        public float getFrameRate() {
            return fps;
        }

        @Override
        public float getTimePerFrame() {
            return 1f / fps;
        }

        @Override
        public void update() {
            ticks++;
        }

        @Override
        public void reset() {
            ticks = 0;
        }
    }
}
