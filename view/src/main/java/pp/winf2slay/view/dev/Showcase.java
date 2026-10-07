package pp.winf2slay.view.dev;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import pp.winf2slay.controller.client.ClientGameLogic;
import pp.winf2slay.controller.client.event.ActionStateChangedEvent;
import pp.winf2slay.controller.client.event.CardMovedEvent;
import pp.winf2slay.controller.client.event.CardsMovedEvent;
import pp.winf2slay.controller.client.event.ChallengeModifiedEvent;
import pp.winf2slay.controller.client.event.ChallengeResolvedEvent;
import pp.winf2slay.controller.client.event.ChallengeRolledEvent;
import pp.winf2slay.controller.client.event.DiceModifiedEvent;
import pp.winf2slay.controller.client.event.DiceRolledEvent;
import pp.winf2slay.controller.client.event.EffectActivatedEvent;
import pp.winf2slay.controller.client.event.GameOverEvent;
import pp.winf2slay.controller.client.event.GameStartedEvent;
import pp.winf2slay.controller.client.event.MonsterAttackedEvent;
import pp.winf2slay.controller.client.event.MulliganEvent;
import pp.winf2slay.controller.client.event.RollResolvedEvent;
import pp.winf2slay.controller.client.event.TurnStartedEvent;
import pp.winf2slay.controller.message.CardPosition;
import pp.winf2slay.controller.message.MoveCause;
import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.controller.message.server.BCCardMoved;
import pp.winf2slay.controller.message.server.BCCardsMoved;
import pp.winf2slay.controller.message.server.BCChallengeModified;
import pp.winf2slay.controller.message.server.BCChallengeResolved;
import pp.winf2slay.controller.message.server.BCChallengeRolled;
import pp.winf2slay.controller.message.server.BCMulligan;
import pp.winf2slay.controller.message.server.BCRollResolved;
import pp.winf2slay.controller.message.server.ClientSync;
import pp.winf2slay.controller.message.server.PlayerSnapshot;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Challenge;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Leader;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.card.Spell;
import pp.winf2slay.model.deck.CardCatalog;
import pp.winf2slay.model.die.DiceResult;
import pp.winf2slay.view.WinfToSlayApp;
import pp.winf2slay.view.net.Session;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Effekt-Vorführung für Entwickler ({@code -Dwinf.showcase=true}): Ein erfundener
 * Spielverlauf spielt alle Animationen und Karteneffekte nacheinander ab – ohne
 * Server. So lassen sich Animationen gezielt prüfen.
 */
public class Showcase extends BaseAppState {

    private static final String ME = "Du";
    private static final String ZUSE = "Bot Zuse";
    private static final String LOVELACE = "Bot Lovelace";

    private final WinfToSlayApp app;
    private final Session session;
    private final ClientGameLogic logic;
    private final Map<String, Seat> seats = new LinkedHashMap<>();
    private final List<Card> discard = new ArrayList<>();
    private final Monster[] open = new Monster[3];
    private final List<Monster> monsterDeck = new ArrayList<>(CardCatalog.monsters());
    private final List<Hero> heroes = new ArrayList<>(CardCatalog.heroes());
    private final List<Runnable> steps = new ArrayList<>();
    private int supportCount = 80;
    private String active = ME;
    private int step;
    private float wait = 1.5f;

    /**
     * Ein Spielerplatz des erfundenen Spiels.
     */
    private static final class Seat {
        private final String name;
        private final boolean bot;
        private final List<Card> hand = new ArrayList<>();
        private final Hero[] group = new Hero[5];
        private final Monster[] monsters = new Monster[3];
        private Leader leader;
        private Card hover;

        private Seat(String name, boolean bot) {
            this.name = name;
            this.bot = bot;
        }

        private int slotOf(Hero h) {
            for (int i = 0; i < group.length; i++) if (h.equals(group[i])) return i;
            return -1;
        }

        private void add(Hero h) {
            for (int i = 0; i < group.length; i++) {
                if (group[i] == null) {
                    group[i] = h;
                    return;
                }
            }
        }
    }

    /**
     * @param app Anwendung
     */
    public Showcase(WinfToSlayApp app) {
        this.app = app;
        this.session = new Session(app, Session.Mode.JOIN, ME);
        this.logic = session.logic();
        setup();
        script();
    }

    private Hero hero() {
        return heroes.removeFirst();
    }

    private void setup() {
        List<Leader> leaders = CardCatalog.leaders();
        Seat me = new Seat(ME, false);
        me.leader = leaders.get(0);
        me.group[0] = hero();
        me.group[1] = hero();
        me.hand.addAll(List.of(hero(), new Challenge(), new Modification(2), CardCatalog.spells().get(0),
                               CardCatalog.spells().get(5), CardCatalog.spells().get(2)));
        Seat zuse = new Seat(ZUSE, true);
        zuse.leader = leaders.get(1);
        zuse.group[0] = hero();
        zuse.group[1] = hero();
        zuse.group[2] = hero();
        zuse.hand.addAll(List.of(new Challenge(), hero(), hero(), hero(), new Modification(-2)));
        Seat lovelace = new Seat(LOVELACE, true);
        lovelace.leader = leaders.get(3);
        lovelace.group[0] = hero();
        lovelace.group[1] = hero();
        lovelace.hand.addAll(List.of(new Modification(2), hero(), hero(), hero()));
        for (Seat s : List.of(me, zuse, lovelace)) seats.put(s.name, s);
        for (int i = 0; i < 3; i++) open[i] = monsterDeck.removeFirst();
        discard.add(hero());
        logic.getModel().setMyName(ME);
        sync();
    }

    private void sync() {
        List<PlayerSnapshot> players = new ArrayList<>();
        for (Seat s : seats.values()) {
            players.add(new PlayerSnapshot(s.name, s.bot, s.hand.size(), s.hover, s.group.clone(), s.monsters.clone(),
                                           s.leader, 0, 0, 0));
        }
        logic.getModel().apply(new ClientSync(ME, players, seats.get(ME).hand, active,
                                              discard.isEmpty() ? null : discard.getLast(), discard.size(),
                                              supportCount, monsterDeck.size(), open.clone()));
    }

    private void move(String actor, String owner, Card card, CardPosition from, CardPosition to, MoveCause cause) {
        sync();
        logic.notify(new CardMovedEvent(new BCCardMoved(actor, owner, card, from, to, cause)));
    }

    private Seat seat(String name) {
        return seats.get(name);
    }

    private void play(String player, Card card) {
        Seat s = seat(player);
        s.hand.remove(card);
        s.hover = card;
        move(player, player, card, CardPosition.PLAYER_HAND, CardPosition.PLAYER_HOVER, MoveCause.PLAY);
    }

    private void spellDone(String player) {
        Seat s = seat(player);
        Card spell = s.hover;
        s.hover = null;
        discard.add(spell);
        move(player, player, spell, CardPosition.PLAYER_HOVER, CardPosition.DISCARD_PILE, MoveCause.PLAY_SUCCEEDED);
    }

    private void destroy(String caster, String owner, Hero hero, MoveCause cause) {
        Seat s = seat(owner);
        int slot = s.slotOf(hero);
        if (slot >= 0) s.group[slot] = null;
        discard.add(hero);
        move(caster, owner, hero, CardPosition.PLAYER_GROUP, CardPosition.DISCARD_PILE, cause);
    }

    private void script() {
        Seat me = seat(ME);
        Seat zuse = seat(ZUSE);
        Seat lovelace = seat(LOVELACE);
        Hero newHero = (Hero) me.hand.getFirst();
        steps.add(() -> {
            logic.getModel().setActivePlayer(ME);
            logic.getModel().startMyTurn();
            logic.notify(new TurnStartedEvent(ME, true));
            logic.notify(new ActionStateChangedEvent(true, 3));
        });
        steps.add(() -> {
            Hero drawn = hero();
            me.hand.add(drawn);
            supportCount--;
            move(ME, ME, drawn, CardPosition.SUPPORT_DECK, CardPosition.PLAYER_HAND, MoveCause.DRAW);
        });
        steps.add(() -> play(ME, newHero));
        steps.add(() -> {
            Card challenge = zuse.hand.removeFirst();
            discard.add(challenge);
            move(ZUSE, ZUSE, challenge, CardPosition.PLAYER_HAND, CardPosition.DISCARD_PILE, MoveCause.CHALLENGE);
        });
        DiceResult a = new DiceResult(ME, 5, 3, 0, 0);
        DiceResult b = new DiceResult(ZUSE, 2, 3, 0, 0);
        steps.add(() -> {
            sync();
            logic.notify(new ChallengeRolledEvent(new BCChallengeRolled(ME, a, ZUSE, b)));
        });
        steps.add(() -> {
            Card mod = lovelace.hand.removeFirst();
            discard.add(mod);
            sync();
            logic.notify(new ChallengeModifiedEvent(new BCChallengeModified(LOVELACE, (Modification) mod, ZUSE, a,
                                                                            b.modifiedBy(2))));
        });
        steps.add(() -> {
            sync();
            logic.notify(new ChallengeResolvedEvent(new BCChallengeResolved(ME, ZUSE, true)));
        });
        steps.add(() -> {
            me.hover = null;
            me.add(newHero);
            move(ME, ME, newHero, CardPosition.PLAYER_HOVER, CardPosition.PLAYER_GROUP, MoveCause.PLAY_SUCCEEDED);
        });
        steps.add(() -> {
            sync();
            logic.notify(new EffectActivatedEvent(ME, newHero));
            DiceResult r = new DiceResult(ME, 4, 3, 0, 0);
            logic.notify(new DiceRolledEvent(r, RollPurpose.HERO_EFFECT, newHero.getThreshold(), true));
            Card mod = me.hand.stream().filter(Modification.class::isInstance).findFirst().orElseThrow();
            me.hand.remove(mod);
            discard.add(mod);
            sync();
            logic.notify(new DiceModifiedEvent(ME, (Modification) mod, r.modifiedBy(2)));
            logic.notify(new RollResolvedEvent(new BCRollResolved(ME, RollPurpose.HERO_EFFECT, newHero, true, 9)));
        });
        steps.add(() -> destroy(ME, ZUSE, zuse.group[0], MoveCause.EFFECT_DESTROY));
        Spell mortar = CardCatalog.spells().get(5);
        steps.add(() -> play(ME, me.hand.stream().filter(mortar::equals).findFirst().orElseThrow()));
        steps.add(() -> {
            sync();
            logic.notify(new EffectActivatedEvent(ME, mortar));
            destroy(ME, LOVELACE, lovelace.group[0], MoveCause.EFFECT_DESTROY_RANDOM);
        });
        steps.add(() -> destroy(ME, ZUSE, zuse.group[1], MoveCause.EFFECT_DESTROY_RANDOM));
        steps.add(() -> spellDone(ME));
        Spell heli = CardCatalog.spells().get(0);
        steps.add(() -> play(ME, me.hand.stream().filter(heli::equals).findFirst().orElseThrow()));
        steps.add(() -> {
            sync();
            logic.notify(new EffectActivatedEvent(ME, heli));
            List<Hero> killed = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                if (zuse.group[i] != null) {
                    killed.add(zuse.group[i]);
                    discard.add(zuse.group[i]);
                    zuse.group[i] = null;
                }
            }
            sync();
            logic.notify(new CardsMovedEvent(BCCardsMoved.of(Map.of(ZUSE, killed), CardPosition.PLAYER_GROUP,
                                                             CardPosition.DISCARD_PILE,
                                                             MoveCause.EFFECT_DESTROY_PARTY)));
        });
        steps.add(() -> spellDone(ME));
        steps.add(() -> {
            sync();
            logic.notify(new MonsterAttackedEvent(ME, open[1]));
            DiceResult r = new DiceResult(ME, 6, 5, 1, 0);
            logic.notify(new DiceRolledEvent(r, RollPurpose.MONSTER_ATTACK, open[1].getThreshold(),
                                             open[1].isHigherWins()));
            logic.notify(new RollResolvedEvent(new BCRollResolved(ME, RollPurpose.MONSTER_ATTACK, open[1], true, 12)));
            Monster m = open[1];
            me.monsters[0] = m;
            open[1] = monsterDeck.removeFirst();
            move(ME, ME, m, CardPosition.OPEN_MONSTERS, CardPosition.PLAYER_MONSTERS, MoveCause.MONSTER_DEFEATED);
        });
        steps.add(() -> {
            sync();
            logic.notify(new MonsterAttackedEvent(LOVELACE, open[0]));
            DiceResult r = new DiceResult(LOVELACE, 1, 2, 0, 0);
            logic.notify(new DiceRolledEvent(r, RollPurpose.MONSTER_ATTACK, open[0].getThreshold(),
                                             open[0].isHigherWins()));
            logic.notify(new RollResolvedEvent(new BCRollResolved(LOVELACE, RollPurpose.MONSTER_ATTACK, open[0], false, 3)));
            destroy(LOVELACE, LOVELACE, lovelace.group[1], MoveCause.MONSTER_PENALTY);
        });
        steps.add(() -> {
            List<Card> old = new ArrayList<>(zuse.hand);
            zuse.hand.clear();
            discard.addAll(old);
            List<Card> drawn = new ArrayList<>();
            for (int i = 0; i < 5; i++) drawn.add(hero());
            zuse.hand.addAll(drawn);
            supportCount -= 5;
            sync();
            logic.notify(new MulliganEvent(new BCMulligan(ZUSE, old, List.of())));
        });
        steps.add(() -> {
            Map<String, List<Card>> lost = new LinkedHashMap<>();
            for (Seat s : seats.values()) {
                Card c = s.hand.removeLast();
                discard.add(c);
                lost.put(s.name, List.of(c));
            }
            sync();
            logic.notify(new CardsMovedEvent(BCCardsMoved.of(lost, CardPosition.PLAYER_HAND, CardPosition.DISCARD_PILE,
                                                             MoveCause.EFFECT_DISCARD)));
        });
        steps.add(() -> {
            Card c = discard.removeLast();
            me.hand.add(c);
            move(ME, ME, c, CardPosition.DISCARD_PILE, CardPosition.PLAYER_HAND, MoveCause.EFFECT_DRAW_DISCARD);
        });
        Spell bomb = CardCatalog.spells().get(2);
        steps.add(() -> {
            me.hand.add(bomb);
            play(ME, bomb);
        });
        steps.add(() -> {
            sync();
            logic.notify(new EffectActivatedEvent(ME, bomb));
            Map<String, List<Hero>> killed = new LinkedHashMap<>();
            for (Seat s : seats.values()) {
                List<Hero> list = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    if (s.group[i] != null) {
                        list.add(s.group[i]);
                        discard.add(s.group[i]);
                        s.group[i] = null;
                    }
                }
                if (!list.isEmpty()) killed.put(s.name, list);
            }
            sync();
            logic.notify(new CardsMovedEvent(BCCardsMoved.of(killed, CardPosition.PLAYER_GROUP,
                                                             CardPosition.DISCARD_PILE, MoveCause.EFFECT_DESTROY_ALL)));
        });
        steps.add(() -> spellDone(ME));
        steps.add(() -> {
            active = ZUSE;
            sync();
            logic.getModel().setActivePlayer(ZUSE);
            logic.notify(new TurnStartedEvent(ZUSE, false));
        });
        steps.add(() -> logic.notify(new GameOverEvent(ME, "Du hast drei Monster besiegt.", true)));
    }

    /**
     * Startet die Vorführung.
     */
    public void begin() {
        app.startShowcase(session, new GameStartedEvent(List.of(ME, ZUSE, LOVELACE), ME));
    }

    @Override
    protected void initialize(Application application) {
        // nichts zu tun
    }

    @Override
    protected void cleanup(Application application) {
        // nichts zu tun
    }

    @Override
    protected void onEnable() {
        // nichts zu tun
    }

    @Override
    protected void onDisable() {
        // nichts zu tun
    }

    @Override
    public void update(float tpf) {
        if (app.getAnimator().isBusy()) {
            wait = 1.2f;
            return;
        }
        wait -= tpf;
        if (wait > 0 || step >= steps.size()) return;
        steps.get(step++).run();
        wait = 1.2f;
    }
}
