package pp.winf2slay.view.game.card;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Monster;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Erzeugt 3D-Karten und liefert die Kartentexturen.
 */
public class CardFactory {

    private static final Logger LOGGER = System.getLogger(CardFactory.class.getName());

    /** Pfad der normalen Kartenrückseite. */
    public static final String BACK = "cards/back.jpg";
    /** Pfad der Rückseite des Ablagestapels. */
    public static final String BACK_DISCARD = "cards/back_discard.jpg";

    private static final ColorRGBA EDGE_COLOR = new ColorRGBA(0.16f, 0.1f, 0.07f, 1f);
    private static final ColorRGBA MONSTER_BACK_TINT = new ColorRGBA(1f, 0.72f, 0.68f, 1f);

    private final AssetManager assets;

    /**
     * @param assets Asset-Manager
     */
    public CardFactory(AssetManager assets) {
        this.assets = assets;
    }

    /**
     * @return Asset-Manager
     */
    public AssetManager assets() {
        return assets;
    }

    /**
     * Liefert den Texturpfad der Vorderseite.
     *
     * @param card Karte
     * @return Pfad
     */
    public static String frontPath(Card card) {
        return "cards/" + card.getName() + ".jpg";
    }

    /**
     * Lädt eine Textur (mit Mipmaps, anisotrop gefiltert).
     *
     * @param path Pfad
     * @return Textur
     */
    public Texture texture(String path) {
        try {
            TextureKey key = new TextureKey(path, true);
            key.setGenerateMips(true);
            Texture t = assets.loadTexture(key);
            t.setAnisotropicFilter(8);
            return t;
        }
        catch (AssetNotFoundException e) {
            LOGGER.log(Level.WARNING, "Kartenbild fehlt: {0}", path);
            return assets.loadTexture(new TextureKey(BACK, true));
        }
    }

    /**
     * @param card Karte
     * @return Textur der Vorderseite
     */
    public Texture front(Card card) {
        return texture(frontPath(card));
    }

    /**
     * Erzeugt eine aufgedeckte Karte.
     *
     * @param card Karte
     * @return Kartenknoten
     */
    public CardNode create(Card card) {
        CardNode node = new CardNode(this, CardSize.of(card), card);
        node.setFrontTexture(front(card));
        return node;
    }

    /**
     * Erzeugt eine verdeckte Karte (z. B. gegnerische Handkarten).
     *
     * @param size    Format
     * @param monster {@code true} für Monsterrückseiten
     * @return Kartenknoten ohne bekannte Vorderseite
     */
    public CardNode createHidden(CardSize size, boolean monster) {
        CardNode node = new CardNode(this, size, null);
        node.setFrontTexture(texture(BACK));
        if (monster) node.tintBack(MONSTER_BACK_TINT);
        return node;
    }

    /**
     * @param card Karte oder {@code null}
     * @return Kartenknoten, verdeckt falls {@code card == null}
     */
    public CardNode createOrHidden(Card card) {
        return card == null ? createHidden(CardSize.SMALL, false) : create(card);
    }

    /**
     * @return neues Material für Kartenflächen
     */
    Material cardMaterial(Texture texture) {
        Material m = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.White.clone());
        m.setColor("Ambient", ColorRGBA.White.clone());
        m.setColor("Specular", new ColorRGBA(0.25f, 0.22f, 0.18f, 1f));
        m.setFloat("Shininess", 24f);
        if (texture != null) m.setTexture("DiffuseMap", texture);
        return m;
    }

    /**
     * @return neues Material für Kartenränder
     */
    Material edgeMaterial() {
        Material m = cardMaterial(null);
        m.setColor("Diffuse", EDGE_COLOR.clone());
        m.setColor("Ambient", EDGE_COLOR.clone());
        return m;
    }

    /**
     * @return Textur der Rückseite
     */
    Texture backTexture() {
        return texture(BACK);
    }

    /**
     * @param monster {@code true} für Monsterkarten
     * @return Farbe der Rückseite
     */
    static ColorRGBA backTint(boolean monster) {
        return monster ? MONSTER_BACK_TINT.clone() : ColorRGBA.White.clone();
    }

    /**
     * @param card Karte
     * @return {@code true} für Monsterkarten
     */
    static boolean isMonster(Card card) {
        return card instanceof Monster;
    }
}
