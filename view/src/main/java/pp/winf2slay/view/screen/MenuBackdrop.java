package pp.winf2slay.view.screen;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.deck.CardCatalog;
import pp.winf2slay.view.WinfToSlayApp;
import pp.winf2slay.view.game.card.CardFactory;
import pp.winf2slay.view.game.card.CardNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Hintergrund der Menüs: Karten liegen auf dem Tisch, einige schweben im Kreis,
 * und die Kamera umrundet langsam den Tisch.
 */
public class MenuBackdrop extends BaseAppState {

    private final Node decor = new Node("menu-decor");
    private final List<CardNode> floating = new ArrayList<>();
    private final List<float[]> orbits = new ArrayList<>();
    private float time;

    @Override
    protected void initialize(Application application) {
        WinfToSlayApp app = (WinfToSlayApp) application;
        CardFactory factory = app.getCards();
        Random random = new Random(7);

        List<Card> heroes = new ArrayList<>(CardCatalog.heroes());
        Collections.shuffle(heroes, random);
        for (int i = 0; i < 5; i++) {
            CardNode card = factory.create(heroes.get(i));
            float angle = (i - 2) * 0.16f;
            card.setLocalTranslation(-6 + (i - 2) * 5.2f, 0.1f + i * 0.02f, 22 + Math.abs(i - 2) * 1.2f);
            card.setLocalRotation(new Quaternion().fromAngleAxis(-angle, Vector3f.UNIT_Y));
            decor.attachChild(card);
        }
        List<Card> monsters = new ArrayList<>(CardCatalog.monsters());
        Collections.shuffle(monsters, random);
        for (int i = 0; i < 3; i++) {
            CardNode card = factory.create(monsters.get(i));
            card.setLocalTranslation((i - 1) * 9.6f, 0.1f, -2);
            decor.attachChild(card);
        }
        for (int i = 0; i < 2; i++) {
            CardNode back = factory.createHidden(pp.winf2slay.view.game.card.CardSize.SMALL, false);
            back.setLocalTranslation(22 + i * 0.5f, 0.1f + i * 0.08f, -2 + i * 0.3f);
            decor.attachChild(back);
        }
        List<Card> others = new ArrayList<>(CardCatalog.leaders());
        others.addAll(CardCatalog.spells());
        Collections.shuffle(others, random);
        for (int i = 0; i < 6; i++) {
            CardNode card = factory.create(others.get(i));
            floating.add(card);
            orbits.add(new float[]{i * FastMath.TWO_PI / 6, 34 + random.nextFloat() * 10, 14 + random.nextFloat() * 8,
                                   0.12f + random.nextFloat() * 0.06f});
            decor.attachChild(card);
        }
    }

    @Override
    protected void cleanup(Application application) {
        decor.removeFromParent();
    }

    @Override
    protected void onEnable() {
        WinfToSlayApp app = (WinfToSlayApp) getApplication();
        app.getTable().getDecor().attachChild(decor);
        app.getRig().menuOrbit();
    }

    @Override
    protected void onDisable() {
        decor.removeFromParent();
    }

    @Override
    public void update(float tpf) {
        time += tpf;
        for (int i = 0; i < floating.size(); i++) {
            float[] o = orbits.get(i);
            float a = o[0] + time * o[3];
            CardNode card = floating.get(i);
            card.setLocalTranslation(FastMath.cos(a) * o[1], o[2] + FastMath.sin(time * 0.9f + i) * 1.5f,
                                     FastMath.sin(a) * o[1] * 0.7f);
            card.setLocalRotation(new Quaternion().fromAngles(-0.9f + FastMath.sin(time * 0.7f + i) * 0.2f,
                                                              -a + FastMath.HALF_PI, FastMath.sin(time + i) * 0.15f));
        }
    }
}
