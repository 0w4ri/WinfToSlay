package pp.winf2slay.view.game;

import com.jme3.effect.ParticleEmitter;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.view.anim.Easing;
import pp.winf2slay.view.anim.Tween;
import pp.winf2slay.view.anim.Tweens;
import pp.winf2slay.view.audio.Sfx;
import pp.winf2slay.view.game.board.BoardView;
import pp.winf2slay.view.game.card.CardNode;
import pp.winf2slay.view.game.card.CardSize;
import pp.winf2slay.view.game.card.CardSprite;
import pp.winf2slay.view.game.card.HandView;
import pp.winf2slay.view.ui.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Bewegungsabläufe von Karten zwischen Stapeln, Händen und Plätzen.
 *
 * <p>Alle Methoden liefern {@link Tween}s für die Hauptspur. Karten werden erst
 * beim Start des jeweiligen Schritts aus ihren Plätzen gelöst, damit Abläufe
 * beliebig aneinandergereiht werden können.</p>
 */
public class CardMoves {

    private static final Quaternion FACE_DOWN = new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Z);

    private final Stage stage;
    private final Random random = new Random();

    /**
     * @param stage Bausteine der Spielansicht
     */
    public CardMoves(Stage stage) {
        this.stage = stage;
    }

    private BoardView board() {
        return stage.board();
    }

    private HandView hand() {
        return stage.hand();
    }

    // ------------------------------------------------------------------
    // Grundbewegung
    // ------------------------------------------------------------------

    /**
     * Fliegt eine Karte im Bogen an ein Ziel.
     *
     * @param node    Karte
     * @param target  Ziel (Lage im Spielfeld)
     * @param seconds Dauer
     * @param arc     Bogenhöhe
     * @param easing  Verlauf
     * @return Tween
     */
    public Tween fly(CardNode node, Transform target, float seconds, float arc, Easing easing) {
        return Tweens.defer(() -> {
            Transform from = node.getLocalTransform().clone();
            Transform to = target.clone();
            return Tweens.lerp(seconds, easing, t -> {
                Transform tr = BoardView.interpolate(from, to, t);
                tr.getTranslation().y += 4 * arc * t * (1 - t);
                node.setLocalTransform(tr);
            });
        });
    }

    private Tween fly(CardNode node, Transform target, float seconds, float arc) {
        return fly(node, target, seconds, arc, Easing.IN_OUT_CUBIC);
    }

    private Transform deckTop(Vector3f top, boolean faceDown) {
        return new Transform(top.clone(), faceDown ? FACE_DOWN.clone() : new Quaternion());
    }

    private Transform discardTarget() {
        Transform t = new Transform(board().discard().topPosition().add(0, CardSize.THICKNESS, 0), new Quaternion());
        t.getRotation().fromAngleAxis((random.nextFloat() - 0.5f) * 0.2f, Vector3f.UNIT_Y);
        return t;
    }

    // ------------------------------------------------------------------
    // Karten aus Händen lösen
    // ------------------------------------------------------------------

    /**
     * Holt eine Karte aus der eigenen Hand als 3D-Karte vor die Kamera.
     *
     * @param card Karte
     * @return Kartenknoten
     */
    public CardNode fromMyHand(Card card) {
        CardSprite sprite = hand().take(card);
        CardNode node = board().spawn(card);
        Vector3f screen;
        float height;
        float roll = 0;
        if (sprite != null) {
            screen = hand().screenPosition(sprite);
            height = hand().screenCardHeight() * sprite.getLocalScale().x;
            roll = sprite.getLocalRotation().toAngles(null)[2];
            sprite.removeFromParent();
        }
        else {
            screen = new Vector3f(stage.bridge().screenWidth() / 2, 0, 0);
            height = hand().screenCardHeight();
        }
        node.setLocalTransform(stage.bridge().facingCamera(screen, height, node.getSize().getHeight(), roll));
        return node;
    }

    /**
     * Holt eine Karte aus der verdeckten Hand eines Gegners.
     *
     * @param seat Platz
     * @param card Karte (aufgedeckt, falls bekannt) oder {@code null}
     * @return Kartenknoten (verdeckt liegend, wird beim Fliegen umgedreht)
     */
    public CardNode fromOpponentHand(int seat, Card card) {
        CardNode hidden = board().detachHandCard(seat);
        if (card == null) return hidden;
        CardNode real = board().spawn(card);
        Transform t = hidden.getLocalTransform().clone();
        t.getRotation().multLocal(FACE_DOWN);
        real.setLocalTransform(t);
        hidden.removeFromParent();
        return real;
    }

    /**
     * Holt eine Karte aus der Hand eines Spielers.
     *
     * @param owner Spieler
     * @param card  Karte
     * @return Kartenknoten
     */
    public CardNode fromHand(String owner, Card card) {
        if (stage.isMe(owner)) return fromMyHand(card);
        return fromOpponentHand(stage.seat(owner), card);
    }

    // ------------------------------------------------------------------
    // Ziehen
    // ------------------------------------------------------------------

    /**
     * Eine Karte wandert vom Nachziehstapel (oder Ablagestapel) auf die Hand.
     *
     * @param owner       Empfänger
     * @param card        Karte (bei Gegnern evtl. {@code null})
     * @param magic       mit Funkenspur (Karteneffekt)
     * @param fromDiscard vom Ablagestapel statt vom Nachziehstapel
     * @return Tween
     */
    public Tween drawToHand(String owner, Card card, boolean magic, boolean fromDiscard) {
        return Tweens.defer(() -> {
            boolean me = stage.isMe(owner);
            int seat = stage.seat(owner);
            CardNode node;
            if (fromDiscard && card != null) {
                node = board().spawn(card);
                node.setLocalTransform(deckTop(board().discard().topPosition(), false));
                CardNode top = board().discard().getTop();
                if (top != null && card.equals(top.getCard())) top.setCullHint(com.jme3.scene.Spatial.CullHint.Always);
            }
            else if (me && card != null) {
                node = board().spawn(card);
                node.setLocalTransform(deckTop(board().supportDeck().topPosition(), true));
            }
            else {
                node = board().spawnHidden(CardSize.SMALL, false);
                node.setLocalTransform(deckTop(board().supportDeck().topPosition(), false));
            }
            ColorRGBA trailColor = fromDiscard ? new ColorRGBA(0.4f, 1f, 0.6f, 0.9f) : Theme.withAlpha(Theme.BLUE, 0.9f);
            ParticleEmitter trail = magic || fromDiscard ? stage.fx().trail(node, trailColor) : null;
            List<Tween> steps = new ArrayList<>();
            steps.add(Tweens.call(() -> stage.sounds().play(Sfx.CARD_FLIP, 0.8f, 0.1f)));
            if (fromDiscard) {
                steps.add(Tweens.lerp(0.45f, Easing.OUT_CUBIC, t -> node.setLocalTranslation(
                        board().discard().topPosition().add(0, t * 7f, 0))));
            }
            if (me && card != null) {
                CardSprite slot = hand().reserve(card);
                Transform target = stage.bridge().facingCamera(hand().screenTarget(slot), hand().screenCardHeight(),
                                                               node.getSize().getHeight(), 0);
                steps.add(Tweens.call(() -> stage.sounds().play(Sfx.WHOOSH, 0.8f, 0.15f)));
                steps.add(fly(node, target, 0.55f, 4f, Easing.IN_OUT_CUBIC));
                steps.add(Tweens.call(() -> {
                    stage.fx().stopTrail(trail);
                    node.removeFromParent();
                    hand().reveal(slot);
                    if (magic) stage.fx().sparks(node.getWorldTranslation(), trailColor, 10, 4f);
                }));
            }
            else {
                int count = board().handSize(seat) + 1;
                Transform target = board().layout().handCard(seat, count - 1, count);
                steps.add(Tweens.call(() -> stage.sounds().play(Sfx.WHOOSH, 0.6f, 0.15f)));
                steps.add(fly(node, target, 0.6f, 6f));
                steps.add(Tweens.call(() -> {
                    stage.fx().stopTrail(trail);
                    CardNode back = node;
                    if (node.getCard() != null) {
                        back = board().spawnHidden(CardSize.SMALL, false);
                        back.setLocalTransform(node.getLocalTransform());
                        node.removeFromParent();
                    }
                    board().placeHandCard(seat, back);
                }));
            }
            return Tweens.seq(steps);
        });
    }

    // ------------------------------------------------------------------
    // Ausspielen
    // ------------------------------------------------------------------

    /**
     * Karte wird aus der Hand ausgespielt und schwebt über dem Platz.
     *
     * @param owner Spieler
     * @param card  Karte
     * @return Tween
     */
    public Tween play(String owner, Card card) {
        return Tweens.defer(() -> {
            int seat = stage.seat(owner);
            CardNode node = fromHand(owner, card);
            Transform hover = board().layout().hover(seat);
            Transform high = hover.clone();
            high.getTranslation().y += 6;
            high.setScale(hover.getScale().mult(1.35f));
            return Tweens.seq(
                    Tweens.call(() -> stage.sounds().play(Sfx.WHOOSH, 1f, 0.1f)),
                    fly(node, high, 0.55f, 6f, Easing.OUT_CUBIC),
                    Tweens.call(() -> {
                        node.setHighlight(Theme.withAlpha(stage.color(owner), 0.9f), true);
                        stage.fx().twinkle(node.getWorldTranslation(), stage.color(owner));
                    }),
                    fly(node, hover, 0.35f, 0f, Easing.IN_OUT_CUBIC),
                    Tweens.call(() -> {
                        stage.sounds().play(Sfx.CARD_PLACE, 0.6f, 0.1f);
                        board().placeHover(seat, node);
                    }));
        });
    }

    /**
     * Ein ausgespielter Held zieht in die Gruppe ein.
     *
     * @param owner Spieler
     * @param hero  Held
     * @return Tween
     */
    public Tween heroJoins(String owner, Hero hero) {
        return Tweens.defer(() -> {
            int seat = stage.seat(owner);
            CardNode node = board().detachHover(seat);
            if (node == null) {
                node = board().spawn(hero);
                node.setLocalTransform(board().layout().hover(seat));
            }
            CardNode card = node;
            int slot = slotOf(owner, hero);
            Transform target = board().layout().hero(seat, slot);
            Transform lift = target.clone();
            lift.getTranslation().y += 5;
            return Tweens.seq(
                    fly(card, lift, 0.35f, 2f, Easing.OUT_CUBIC),
                    fly(card, target, 0.22f, 0f, Easing.IN_CUBIC),
                    Tweens.call(() -> {
                        card.setHighlight(null, false);
                        board().placeHero(seat, slot, card);
                        stage.sounds().play(Sfx.CARD_PLACE, 1f, 0.05f);
                        stage.fx().sparks(card.getWorldTranslation(), Theme.GOLD_LIGHT, 22, 9f);
                        stage.rig().shake(0.25f, 0.15f);
                    }),
                    stage.fx().shockwave(target.getTranslation(), 7f, Theme.withAlpha(stage.color(owner), 0.8f), 0.45f));
        });
    }

    private int slotOf(String owner, Hero hero) {
        return stage.model().getPlayer(owner).map(p -> {
            Hero[] group = p.getGroup();
            for (int i = 0; i < group.length; i++) if (hero.equals(group[i])) return i;
            for (int i = 0; i < group.length; i++) if (group[i] == null) return i;
            return 0;
        }).orElse(0);
    }

    // ------------------------------------------------------------------
    // Ablegen
    // ------------------------------------------------------------------

    /**
     * Fliegt eine Karte auf den Ablagestapel.
     *
     * @param node    Karte
     * @param seconds Dauer
     * @param spin    mit Drehung (weggeschleudert)
     * @return Tween
     */
    public Tween toDiscard(CardNode node, float seconds, boolean spin) {
        return Tweens.defer(() -> {
            Transform from = node.getLocalTransform().clone();
            Transform to = discardTarget();
            float turns = spin ? (random.nextBoolean() ? 1 : -1) * FastMath.TWO_PI : 0;
            return Tweens.seq(
                    Tweens.call(() -> {
                        node.setHighlight(null, false);
                        stage.sounds().play(Sfx.WHOOSH, 0.6f, 0.2f);
                    }),
                    Tweens.lerp(seconds, Easing.IN_OUT_CUBIC, t -> {
                        Transform tr = BoardView.interpolate(from, to, t);
                        tr.getTranslation().y += 4 * (spin ? 14 : 5) * t * (1 - t);
                        if (spin) tr.getRotation().multLocal(new Quaternion().fromAngleAxis(turns * t, Vector3f.UNIT_Y));
                        node.setLocalTransform(tr);
                    }),
                    Tweens.call(() -> {
                        stage.sounds().play(Sfx.CARD_PLACE, 0.6f, 0.1f);
                        board().placeOnDiscard(node);
                    }));
        });
    }

    /**
     * Karte aus einer Hand direkt auf den Ablagestapel (Herausforderung, Abwerfen).
     *
     * @param owner Spieler
     * @param card  Karte
     * @return Tween
     */
    public Tween handToDiscard(String owner, Card card) {
        return Tweens.defer(() -> {
            CardNode node = fromHand(owner, card);
            return toDiscard(node, 0.6f, false);
        });
    }

    /**
     * Ein Held verlässt seinen Platz in Richtung Ablagestapel.
     *
     * @param owner Besitzer
     * @param hero  Held
     * @param spin  weggeschleudert
     * @return Tween
     */
    public Tween heroToDiscard(String owner, Hero hero, boolean spin) {
        return Tweens.defer(() -> {
            CardNode node = board().detachHero(stage.seat(owner), hero);
            if (node == null) return Tweens.NONE;
            return toDiscard(node, spin ? 0.8f : 0.6f, spin);
        });
    }

    // ------------------------------------------------------------------
    // Monster
    // ------------------------------------------------------------------

    /**
     * Ein besiegtes Monster wandert zu den Trophäen des Spielers.
     *
     * @param owner   Spieler
     * @param monster Monster
     * @return Tween
     */
    public Tween trophy(String owner, Monster monster) {
        return Tweens.defer(() -> {
            int seat = stage.seat(owner);
            CardNode node = board().detachOpenMonster(monster);
            if (node == null) return Tweens.NONE;
            int index = board().trophyCount(seat);
            Transform target = board().layout().trophy(seat, index);
            Transform up = node.getLocalTransform().clone();
            up.getTranslation().y += 8;
            up.setScale(1.25f);
            return Tweens.seq(
                    Tweens.call(() -> node.setHighlight(Theme.withAlpha(Theme.GOLD, 0.95f), true)),
                    fly(node, up, 0.4f, 0f, Easing.OUT_BACK),
                    Tweens.delay(0.25f),
                    fly(node, target, 0.7f, 10f),
                    Tweens.call(() -> {
                        node.setHighlight(null, false);
                        board().placeTrophy(seat, node);
                        stage.fx().twinkle(node.getWorldTranslation(), Theme.GOLD_LIGHT);
                        stage.sounds().play(Sfx.SPARKLE);
                    }));
        });
    }

    /**
     * Deckt Monster für leere Plätze in der Tischmitte auf.
     *
     * @return Tween
     */
    public Tween revealOpenMonsters() {
        return Tweens.defer(() -> {
            Monster[] open = stage.model().getOpenMonsters();
            List<Tween> reveals = new ArrayList<>();
            for (int i = 0; open != null && i < open.length; i++) {
                if (open[i] == null || board().openMonster(i) != null) continue;
                int index = i;
                CardNode node = board().spawn(open[i]);
                node.setLocalTransform(deckTop(board().monsterDeck().topPosition(), true));
                Transform target = board().layout().openMonster(i);
                Transform high = target.clone();
                high.getTranslation().y += 7;
                high.setScale(1.2f);
                reveals.add(Tweens.seq(
                        Tweens.call(() -> stage.sounds().play(Sfx.CARD_FLIP, 1f, 0.1f)),
                        fly(node, high, 0.55f, 4f, Easing.OUT_CUBIC),
                        Tweens.call(() -> stage.fx().sparks(high.getTranslation(), new ColorRGBA(1f, 0.35f, 0.25f, 1f),
                                                             20, 7f)),
                        fly(node, target, 0.3f, 0f, Easing.IN_CUBIC),
                        Tweens.call(() -> {
                            board().placeOpenMonster(index, node);
                            stage.sounds().play(Sfx.CARD_PLACE);
                        })));
            }
            return Tweens.stagger(0.2f, reveals);
        });
    }
}
