package pp.winf2slay.view.game.board;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.MouseEventControl;
import pp.winf2slay.controller.client.ClientGameState;
import pp.winf2slay.controller.message.server.PlayerSnapshot;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.field.MonsterField;
import pp.winf2slay.view.anim.Animator;
import pp.winf2slay.view.anim.Easing;
import pp.winf2slay.view.anim.Tweens;
import pp.winf2slay.view.game.BoardLayout;
import pp.winf2slay.view.game.card.CardFactory;
import pp.winf2slay.view.game.card.CardNode;
import pp.winf2slay.view.game.card.CardSize;
import pp.winf2slay.view.ui.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Der sichtbare Zustand des Spieltischs.
 *
 * <p>Die Spielereignisse werden von Animationen dargestellt, die Karten aus ihren
 * Plätzen lösen ({@code detach…}), bewegen und wieder ablegen ({@code place…}).
 * Nach jedem Ereignis gleicht {@link #reconcile(ClientGameState)} den Tisch mit
 * dem Modell ab – dadurch stimmt die Anzeige auch dann, wenn eine Animation
 * übersprungen wurde.</p>
 */
public class BoardView {

    /**
     * Art eines Kartenplatzes.
     */
    public enum Kind {
        /** Anführer. */
        LEADER,
        /** Held in der Gruppe. */
        HERO,
        /** Besiegtes Monster. */
        TROPHY,
        /** Gerade ausgespielte Karte. */
        HOVER,
        /** Verdeckte Handkarte eines Gegners. */
        HAND,
        /** Offenes Monster in der Tischmitte. */
        OPEN_MONSTER,
        /** Ablagestapel. */
        DISCARD,
        /** Nachzieh- oder Monsterstapel. */
        DECK
    }

    /**
     * Ort einer Karte.
     *
     * @param kind  Art
     * @param seat  Platz (−1 für die Tischmitte)
     * @param index Index innerhalb des Platzes
     */
    public record Place(Kind kind, int seat, int index) {}

    /**
     * Empfänger von Mausereignissen auf Karten.
     */
    public interface Interaction {
        /**
         * @param card   Karte
         * @param place  Ort
         * @param button Maustaste (0 = links, 1 = rechts)
         */
        void cardClicked(CardNode card, Place place, int button);

        /**
         * @param card    Karte
         * @param place   Ort
         * @param entered {@code true} beim Betreten, {@code false} beim Verlassen
         */
        void cardHovered(CardNode card, Place place, boolean entered);
    }

    private final CardFactory factory;
    private final Node board;
    private final BoardLayout layout;
    private final Animator animator;
    private final Interaction interaction;
    private final List<SeatView> seats = new ArrayList<>();
    private final CardNode[] openMonsters = new CardNode[MonsterField.SIZE];
    private final DeckView supportDeck;
    private final DeckView monsterDeck;
    private final DeckView discard;
    private final MouseHandler mouse = new MouseHandler();

    /**
     * @param factory     Kartenfabrik
     * @param board       Knoten für alle Spielfeldelemente
     * @param seating     Spieler, beginnend mit dem eigenen Platz
     * @param animator    für kleine Übergänge beim Abgleich
     * @param interaction Empfänger von Klicks
     */
    public BoardView(CardFactory factory, Node board, List<String> seating, Animator animator,
                     Interaction interaction) {
        this.factory = factory;
        this.board = board;
        this.layout = new BoardLayout(seating.size());
        this.animator = animator;
        this.interaction = interaction;
        for (int i = 0; i < seating.size(); i++) {
            SeatView seat = new SeatView(factory.assets(), layout, i, seating.get(i), Theme.playerColor(i));
            seats.add(seat);
            board.attachChild(seat.getDecor());
        }
        supportDeck = new DeckView(factory, CardSize.SMALL, false, false, layout.supportDeck());
        monsterDeck = new DeckView(factory, CardSize.BIG, true, false, layout.monsterDeck());
        discard = new DeckView(factory, CardSize.SMALL, false, true, layout.discardPile());
        board.attachChild(supportDeck);
        board.attachChild(monsterDeck);
        board.attachChild(discard);
        MouseEventControl.addListenersToSpatial(supportDeck, mouse);
        MouseEventControl.addListenersToSpatial(discard, mouse);
        MouseEventControl.addListenersToSpatial(monsterDeck, mouse);
    }

    /**
     * @return Tischaufteilung
     */
    public BoardLayout layout() {
        return layout;
    }

    /**
     * @return alle Plätze
     */
    public List<SeatView> seats() {
        return seats;
    }

    /**
     * @param seat Platznummer
     * @return Platz
     */
    public SeatView seat(int seat) {
        return seats.get(seat);
    }

    /**
     * @param player Spielername
     * @return Platznummer oder −1
     */
    public int seatOf(String player) {
        for (SeatView s : seats) {
            if (s.getPlayer().equals(player)) return s.getSeat();
        }
        return -1;
    }

    /** @return Nachziehstapel */
    public DeckView supportDeck() {
        return supportDeck;
    }

    /** @return Monsterstapel */
    public DeckView monsterDeck() {
        return monsterDeck;
    }

    /** @return Ablagestapel */
    public DeckView discard() {
        return discard;
    }

    /**
     * @param index Monsterplatz
     * @return offenes Monster oder {@code null}
     */
    public CardNode openMonster(int index) {
        return openMonsters[index];
    }

    /**
     * @param player Spieler, der am Zug ist
     */
    public void setActive(String player) {
        for (SeatView s : seats) s.setActive(s.getPlayer().equals(player));
    }

    /**
     * @param tpf Zeit seit dem letzten Bild
     */
    public void update(float tpf) {
        for (SeatView s : seats) s.update(tpf);
    }

    // ------------------------------------------------------------------
    // Karten erzeugen
    // ------------------------------------------------------------------

    /**
     * Erzeugt eine aufgedeckte Karte im Spielfeld (noch ohne Platz).
     *
     * @param card Karte
     * @return Kartenknoten
     */
    public CardNode spawn(Card card) {
        CardNode node = factory.create(card);
        register(node);
        return node;
    }

    /**
     * Erzeugt eine verdeckte Karte im Spielfeld (noch ohne Platz).
     *
     * @param size    Format
     * @param monster Monsterrückseite
     * @return Kartenknoten
     */
    public CardNode spawnHidden(CardSize size, boolean monster) {
        CardNode node = factory.createHidden(size, monster);
        register(node);
        return node;
    }

    private void register(CardNode node) {
        board.attachChild(node);
        MouseEventControl.addListenersToSpatial(node, mouse);
    }

    /**
     * @param node Karte
     * @return Ort der Karte oder {@code null}, wenn sie gerade keinen Platz hat
     */
    public Place placeOf(CardNode node) {
        for (SeatView s : seats) {
            if (s.leader == node) return new Place(Kind.LEADER, s.getSeat(), 0);
            for (int i = 0; i < s.heroes.length; i++) {
                if (s.heroes[i] == node) return new Place(Kind.HERO, s.getSeat(), i);
            }
            int t = s.trophies.indexOf(node);
            if (t >= 0) return new Place(Kind.TROPHY, s.getSeat(), t);
            if (s.hover == node) return new Place(Kind.HOVER, s.getSeat(), 0);
            int h = s.hand.indexOf(node);
            if (h >= 0) return new Place(Kind.HAND, s.getSeat(), h);
        }
        for (int i = 0; i < openMonsters.length; i++) {
            if (openMonsters[i] == node) return new Place(Kind.OPEN_MONSTER, -1, i);
        }
        if (discard.getTop() == node) return new Place(Kind.DISCARD, -1, 0);
        return null;
    }

    // ------------------------------------------------------------------
    // Abgleich mit dem Modell
    // ------------------------------------------------------------------

    /**
     * Baut den Tisch ohne Animation nach dem Modell auf.
     *
     * @param model Spielzustand
     */
    public void build(ClientGameState model) {
        reconcile(model, false);
    }

    /**
     * Gleicht den Tisch mit dem Modell ab. Abweichungen werden mit kurzen
     * Übergängen korrigiert.
     *
     * @param model Spielzustand
     */
    public void reconcile(ClientGameState model) {
        reconcile(model, true);
    }

    private void reconcile(ClientGameState model, boolean animate) {
        for (SeatView seat : seats) {
            model.getPlayer(seat.getPlayer()).ifPresent(p -> reconcileSeat(seat, p, seat.getSeat() == 0, animate));
        }
        Monster[] open = model.getOpenMonsters();
        for (int i = 0; i < openMonsters.length; i++) {
            Monster wanted = open != null && i < open.length ? open[i] : null;
            openMonsters[i] = sync(openMonsters[i], wanted, layout.openMonster(i), animate);
        }
        supportDeck.update(model.getSupportCount(), null);
        monsterDeck.update(model.getMonsterDeckCount(), null);
        discard.update(model.getDiscardCount(), model.getDiscardTop());
        registerDeckTop(discard);
    }

    private void registerDeckTop(DeckView deck) {
        CardNode top = deck.getTop();
        if (top != null && top.getControl(MouseEventControl.class) == null)
            MouseEventControl.addListenersToSpatial(top, mouse);
    }

    private void reconcileSeat(SeatView seat, PlayerSnapshot p, boolean me, boolean animate) {
        int s = seat.getSeat();
        seat.leader = sync(seat.leader, p.getLeader(), layout.leader(s), animate);
        Hero[] group = p.getGroup();
        for (int i = 0; i < seat.heroes.length; i++) {
            Hero wanted = group != null && i < group.length ? group[i] : null;
            seat.heroes[i] = sync(seat.heroes[i], wanted, layout.hero(s, i), animate);
        }
        seat.hover = sync(seat.hover, p.getHover(), layout.hover(s), animate);

        List<Monster> monsters = new ArrayList<>();
        if (p.getMonsters() != null) {
            for (Monster m : p.getMonsters()) if (m != null) monsters.add(m);
        }
        for (int i = 0; i < Math.max(monsters.size(), seat.trophies.size()); i++) {
            Monster wanted = i < monsters.size() ? monsters.get(i) : null;
            CardNode current = i < seat.trophies.size() ? seat.trophies.get(i) : null;
            CardNode result = sync(current, wanted, layout.trophy(s, i), animate);
            if (i < seat.trophies.size()) seat.trophies.set(i, result);
            else if (result != null) seat.trophies.add(result);
        }
        seat.trophies.removeIf(Objects::isNull);

        int handCount = me ? 0 : p.getHandCount();
        while (seat.hand.size() > handCount) {
            remove(seat.hand.removeLast(), animate);
        }
        while (seat.hand.size() < handCount) {
            CardNode back = spawnHidden(CardSize.SMALL, false);
            back.setLocalTransform(layout.handCard(s, seat.hand.size(), handCount));
            seat.hand.add(back);
            if (animate) popIn(back);
        }
        layoutHand(seat, animate);
    }

    /**
     * Ordnet die verdeckten Handkarten eines Gegners neu an.
     *
     * @param seat    Platz
     * @param animate weich verschieben
     */
    public void layoutHand(SeatView seat, boolean animate) {
        int n = seat.hand.size();
        for (int i = 0; i < n; i++) {
            moveTo(seat.hand.get(i), layout.handCard(seat.getSeat(), i, n), animate);
        }
    }

    private CardNode sync(CardNode current, Card wanted, Transform slot, boolean animate) {
        if (wanted == null) {
            if (current != null) remove(current, animate);
            return null;
        }
        if (current != null && wanted.equals(current.getCard())) {
            moveTo(current, slot, animate);
            return current;
        }
        if (current != null) remove(current, animate);
        CardNode node = spawn(wanted);
        node.setLocalTransform(slot);
        if (animate) popIn(node);
        return node;
    }

    private void moveTo(CardNode node, Transform target, boolean animate) {
        Transform from = node.getLocalTransform().clone();
        if (from.getTranslation().distanceSquared(target.getTranslation()) < 1e-4f
            && Math.abs(from.getScale().x - target.getScale().x) < 1e-3f) {
            node.setLocalTransform(target);
            return;
        }
        if (!animate) {
            node.setLocalTransform(target);
            return;
        }
        Transform to = target.clone();
        animator.play(Tweens.lerp(0.3f, Easing.OUT_CUBIC, t -> node.setLocalTransform(interpolate(from, to, t))));
    }

    private void popIn(CardNode node) {
        Vector3f scale = node.getLocalScale().clone();
        node.setLocalScale(0.01f);
        animator.play(Tweens.lerp(0.35f, Easing.OUT_BACK, t -> node.setLocalScale(scale.mult(Math.max(0.01f, t)))));
    }

    private void remove(CardNode node, boolean animate) {
        if (!animate) {
            node.removeFromParent();
            return;
        }
        animator.play(Tweens.seq(Tweens.lerp(0.25f, Easing.IN_QUAD, t -> node.setOpacity(1 - t)),
                                 Tweens.call(node::removeFromParent)));
    }

    /**
     * Interpoliert zwischen zwei Lagen.
     *
     * @param a Start
     * @param b Ziel
     * @param t Fortschritt
     * @return Zwischenlage
     */
    public static Transform interpolate(Transform a, Transform b, float t) {
        Transform r = new Transform();
        r.setTranslation(new Vector3f().interpolateLocal(a.getTranslation(), b.getTranslation(), t));
        Quaternion q = new Quaternion();
        q.slerp(a.getRotation(), b.getRotation(), t);
        r.setRotation(q);
        r.setScale(new Vector3f().interpolateLocal(a.getScale(), b.getScale(), t));
        return r;
    }

    // ------------------------------------------------------------------
    // Karten lösen und ablegen (für Animationen)
    // ------------------------------------------------------------------

    /**
     * Löst einen Helden aus seinem Platz.
     *
     * @param seat Platz
     * @param hero Held
     * @return Kartenknoten oder {@code null}
     */
    public CardNode detachHero(int seat, Hero hero) {
        SeatView s = seats.get(seat);
        for (int i = 0; i < s.heroes.length; i++) {
            if (s.heroes[i] != null && hero.equals(s.heroes[i].getCard())) {
                CardNode node = s.heroes[i];
                s.heroes[i] = null;
                return node;
            }
        }
        return null;
    }

    /**
     * @param seat Platz
     * @param hero Held
     * @return Kartenknoten des Helden oder {@code null}
     */
    public CardNode findHero(int seat, Hero hero) {
        if (seat < 0) return null;
        for (CardNode node : seats.get(seat).heroes) {
            if (node != null && hero.equals(node.getCard())) return node;
        }
        return null;
    }

    /**
     * @param seat Platz
     * @return ausgespielte Karte (aus ihrem Platz gelöst) oder {@code null}
     */
    public CardNode detachHover(int seat) {
        SeatView s = seats.get(seat);
        CardNode node = s.hover;
        s.hover = null;
        return node;
    }

    /**
     * @param seat Platz
     * @return ausgespielte Karte oder {@code null}
     */
    public CardNode hover(int seat) {
        return seats.get(seat).hover;
    }

    /**
     * @param monster Monster
     * @return Index des offenen Monsters oder −1
     */
    public int openMonsterIndex(Monster monster) {
        for (int i = 0; i < openMonsters.length; i++) {
            if (openMonsters[i] != null && monster.equals(openMonsters[i].getCard())) return i;
        }
        return -1;
    }

    /**
     * @param monster Monster
     * @return Kartenknoten (aus seinem Platz gelöst) oder {@code null}
     */
    public CardNode detachOpenMonster(Monster monster) {
        int i = openMonsterIndex(monster);
        if (i < 0) return null;
        CardNode node = openMonsters[i];
        openMonsters[i] = null;
        return node;
    }

    /**
     * Entfernt eine verdeckte Handkarte eines Gegners (oder erzeugt eine, falls keine da ist).
     *
     * @param seat Platz
     * @return verdeckte Karte
     */
    public CardNode detachHandCard(int seat) {
        SeatView s = seats.get(seat);
        if (!s.hand.isEmpty()) {
            CardNode node = s.hand.removeLast();
            layoutHand(s, true);
            return node;
        }
        CardNode node = spawnHidden(CardSize.SMALL, false);
        node.setLocalTransform(layout.handCard(seat, 0, 1));
        return node;
    }

    /**
     * @param seat Platz
     * @return Anzahl der verdeckten Handkarten auf dem Tisch
     */
    public int handSize(int seat) {
        return seats.get(seat).hand.size();
    }

    /**
     * @param seat Platz
     * @return Anzahl der Trophäen
     */
    public int trophyCount(int seat) {
        return seats.get(seat).trophies.size();
    }

    /**
     * @param seat Platz
     * @param slot Heldenplatz
     * @param node Karte
     */
    public void placeHero(int seat, int slot, CardNode node) {
        SeatView s = seats.get(seat);
        if (s.heroes[slot] != null && s.heroes[slot] != node) s.heroes[slot].removeFromParent();
        s.heroes[slot] = node;
        node.setLocalTransform(layout.hero(seat, slot));
    }

    /**
     * @param seat Platz
     * @param node Karte
     */
    public void placeHover(int seat, CardNode node) {
        SeatView s = seats.get(seat);
        if (s.hover != null && s.hover != node) s.hover.removeFromParent();
        s.hover = node;
        node.setLocalTransform(layout.hover(seat));
    }

    /**
     * @param seat Platz
     * @param node Monsterkarte
     */
    public void placeTrophy(int seat, CardNode node) {
        SeatView s = seats.get(seat);
        s.trophies.add(node);
        node.setLocalTransform(layout.trophy(seat, s.trophies.size() - 1));
    }

    /**
     * @param index Monsterplatz
     * @param node  Monsterkarte
     */
    public void placeOpenMonster(int index, CardNode node) {
        if (openMonsters[index] != null && openMonsters[index] != node) openMonsters[index].removeFromParent();
        openMonsters[index] = node;
        node.setLocalTransform(layout.openMonster(index));
    }

    /**
     * @param seat Platz
     * @param node verdeckte Karte
     */
    public void placeHandCard(int seat, CardNode node) {
        SeatView s = seats.get(seat);
        s.hand.add(node);
        layoutHand(s, true);
    }

    /**
     * Legt eine Karte auf den Ablagestapel.
     *
     * @param node Karte
     */
    public void placeOnDiscard(CardNode node) {
        discard.adoptTop(node);
        registerDeckTop(discard);
    }

    /**
     * Lage eines Heldenplatzes.
     *
     * @param seat Platz
     * @param slot Heldenplatz
     * @return Lage
     */
    public Transform heroSlot(int seat, int slot) {
        return layout.hero(seat, slot);
    }

    /**
     * Entfernt alle Elemente vom Tisch.
     */
    public void clear() {
        board.detachAllChildren();
        seats.clear();
    }

    /**
     * Leitet Mausereignisse der Karten an die Spielansicht weiter.
     */
    private final class MouseHandler extends DefaultMouseListener {
        @Override
        protected void click(MouseButtonEvent event, Spatial target, Spatial capture) {
            CardNode node = cardOf(target);
            if (node == null) {
                if (isDeck(target)) interaction.cardClicked(null, new Place(Kind.DECK, -1, 0), event.getButtonIndex());
                return;
            }
            Place place = placeOf(node);
            if (place != null) interaction.cardClicked(node, place, event.getButtonIndex());
        }

        @Override
        public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture) {
            CardNode node = cardOf(target);
            if (node == null) return;
            Place place = placeOf(node);
            if (place != null) interaction.cardHovered(node, place, true);
        }

        @Override
        public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture) {
            CardNode node = cardOf(target);
            if (node == null) return;
            interaction.cardHovered(node, placeOf(node), false);
        }

        private CardNode cardOf(Spatial s) {
            Spatial current = s;
            while (current != null) {
                if (current instanceof CardNode card) return card;
                current = current.getParent();
            }
            return null;
        }

        private boolean isDeck(Spatial s) {
            Spatial current = s;
            while (current != null) {
                if (current == supportDeck) return true;
                current = current.getParent();
            }
            return false;
        }
    }
}
