package pp.winf2slay.view.audio;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import pp.winf2slay.view.settings.UserSettings;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Spielt Soundeffekte und die Hintergrundmusik.
 *
 * <p>Die Lautstärken kommen aus den {@link UserSettings} und werden bei jeder
 * Änderung übernommen. Die Musik wird beim Wechsel weich ein- und ausgeblendet.</p>
 */
public class SoundManager extends BaseAppState {

    private static final Logger LOGGER = System.getLogger(SoundManager.class.getName());
    private static final String MUSIC = "music/tavern.ogg";
    private static final float FADE_SECONDS = 1.5f;

    private final UserSettings settings;
    private final Map<Sfx, AudioNode> nodes = new EnumMap<>(Sfx.class);
    private final Random random = new Random();
    private AudioNode music;
    private float musicLevel;      // aktueller Faktor für weiches Ein-/Ausblenden
    private float musicTarget = 1f;

    /**
     * @param settings Benutzereinstellungen
     */
    public SoundManager(UserSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void initialize(Application app) {
        SimpleApplication simple = (SimpleApplication) app;
        for (Sfx sfx : Sfx.values()) {
            try {
                AudioNode node = new AudioNode(app.getAssetManager(), sfx.getPath(), AudioData.DataType.Buffer);
                node.setPositional(false);
                node.setLooping(false);
                simple.getRootNode().attachChild(node);
                nodes.put(sfx, node);
            }
            catch (AssetNotFoundException | IllegalStateException e) {
                LOGGER.log(Level.WARNING, "Sound {0} konnte nicht geladen werden: {1}", sfx, e.getMessage());
            }
        }
        try {
            music = new AudioNode(app.getAssetManager(), MUSIC, AudioData.DataType.Stream);
            music.setPositional(false);
            music.setLooping(true);
            simple.getRootNode().attachChild(music);
        }
        catch (AssetNotFoundException | IllegalStateException e) {
            LOGGER.log(Level.WARNING, "Musik konnte nicht geladen werden: {0}", e.getMessage());
        }
        settings.addListener(this::applyMusicVolume);
    }

    @Override
    protected void cleanup(Application app) {
        if (music != null) music.stop();
        nodes.values().forEach(AudioNode::removeFromParent);
    }

    @Override
    protected void onEnable() {
        if (music != null) {
            musicLevel = 0;
            applyMusicVolume();
            music.play();
        }
    }

    @Override
    protected void onDisable() {
        if (music != null) music.pause();
    }

    /**
     * Spielt einen Effekt ab.
     *
     * @param sfx Effekt
     */
    public void play(Sfx sfx) {
        play(sfx, 1f, 0f);
    }

    /**
     * Spielt einen Effekt ab.
     *
     * @param sfx          Effekt
     * @param volumeFactor zusätzlicher Lautstärkefaktor
     * @param pitchJitter  zufällige Tonhöhenabweichung (z. B. 0,08 für ±8 %)
     */
    public void play(Sfx sfx, float volumeFactor, float pitchJitter) {
        AudioNode node = nodes.get(sfx);
        if (node == null || !isEnabled()) return;
        float volume = sfx.getVolume() * volumeFactor * settings.getEffectsVolume();
        if (volume <= 0.001f) return;
        node.setVolume(volume);
        float pitch = 1f + (pitchJitter > 0 ? (random.nextFloat() * 2 - 1) * pitchJitter : 0);
        node.setPitch(Math.max(0.5f, Math.min(2f, pitch)));
        node.playInstance();
    }

    /**
     * Dämpft die Musik (z. B. während des Spiels) oder hebt sie wieder an.
     *
     * @param factor Zielfaktor 0..1
     */
    public void setMusicLevel(float factor) {
        musicTarget = Math.max(0f, Math.min(1f, factor));
    }

    private void applyMusicVolume() {
        if (music != null)
            music.setVolume(settings.getMusicVolume() * musicLevel * 0.8f);
    }

    @Override
    public void update(float tpf) {
        if (music == null) return;
        if (musicLevel != musicTarget) {
            float step = tpf / FADE_SECONDS;
            musicLevel = musicLevel < musicTarget ? Math.min(musicTarget, musicLevel + step)
                                                  : Math.max(musicTarget, musicLevel - step);
            applyMusicVolume();
        }
        if (music.getStatus() != AudioSource.Status.Playing && isEnabled())
            music.play();
    }
}
