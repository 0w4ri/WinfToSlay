package pp.winf2slay.view.game;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import pp.winf2slay.controller.client.event.ActionStateChangedEvent;
import pp.winf2slay.controller.client.event.CardMovedEvent;
import pp.winf2slay.controller.client.event.CardsMovedEvent;
import pp.winf2slay.controller.client.event.ChallengeModifiedEvent;
import pp.winf2slay.controller.client.event.ChallengeRequestEvent;
import pp.winf2slay.controller.client.event.ChallengeResolvedEvent;
import pp.winf2slay.controller.client.event.ChallengeRolledEvent;
import pp.winf2slay.controller.client.event.DiceModifiedEvent;
import pp.winf2slay.controller.client.event.DiceRolledEvent;
import pp.winf2slay.controller.client.event.EffectActivatedEvent;
import pp.winf2slay.controller.client.event.GameEventListener;
import pp.winf2slay.controller.client.event.GameOverEvent;
import pp.winf2slay.controller.client.event.HeroSelectionRequestEvent;
import pp.winf2slay.controller.client.event.ModifyDoubleRequestEvent;
import pp.winf2slay.controller.client.event.ModifySingleRequestEvent;
import pp.winf2slay.controller.client.event.MonsterAttackedEvent;
import pp.winf2slay.controller.client.event.MulliganEvent;
import pp.winf2slay.controller.client.event.NoticeEvent;
import pp.winf2slay.controller.client.event.PlayerSelectionRequestEvent;
import pp.winf2slay.controller.client.event.RequestClosedEvent;
import pp.winf2slay.controller.client.event.RollResolvedEvent;
import pp.winf2slay.controller.client.event.TurnEndedEvent;
import pp.winf2slay.controller.client.event.TurnStartedEvent;
import pp.winf2slay.controller.message.CardPosition;
import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.controller.message.server.BCCardMoved;
import pp.winf2slay.controller.message.server.BCCardsMoved;
import pp.winf2slay.controller.message.server.BCChallengeModified;
import pp.winf2slay.controller.message.server.BCChallengeResolved;
import pp.winf2slay.controller.message.server.BCChallengeRolled;
import pp.winf2slay.controller.message.server.BCMulligan;
import pp.winf2slay.controller.message.server.BCRollResolved;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.ClassType;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.card.Monster;
import pp.winf2slay.model.card.Spell;
import pp.winf2slay.model.die.DiceResult;
import pp.winf2slay.model.effect.DestroyAllHeroes;
import pp.winf2slay.model.effect.DestroyParty;
import pp.winf2slay.model.effect.DestroyRandomHeroes;
import pp.winf2slay.view.anim.Easing;
import pp.winf2slay.view.anim.Tween;
import pp.winf2slay.view.anim.Tweens;
import pp.winf2slay.view.audio.Sfx;
import pp.winf2slay.view.game.card.CardNode;
import pp.winf2slay.view.ui.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Setzt die Spielereignisse der Client-Logik in Animationen, Effekte, Töne und
 * Protokolleinträge um („Regie“).
 *
 * <p>Alle Abläufe kommen auf die Hauptspur des {@link pp.winf2slay.view.anim.Animator}.
 * Anfragen des Servers werden erst angezeigt, wenn die laufenden Animationen
 * fertig sind.</p>
 */
public class GameDirector implements GameEventListener {

    private final Stage s;
    private final CardMoves moves;
    private final Runnable refresh;
    private final Consumer<List<Hero>> heroSelection;
    private final Consumer<GameOverEvent> gameOver;
    private boolean attackFocus;

    /**
     * @param stage         Bausteine der Spielansicht
     * @param refresh       aktualisiert Oberfläche und Hervorhebungen
     * @param heroSelection schaltet die Heldenauswahl am Tisch ein ({@code null} = aus)
     * @param gameOver      zeigt das Spielende an
     */
    public GameDirector(Stage stage, Runnable refresh, Consumer<List<Hero>> heroSelection,
                        Consumer<GameOverEvent> gameOver) {
        this.s = stage;
        this.moves = new CardMoves(stage);
        this.refresh = refresh;
        this.heroSelection = heroSelection;
        this.gameOver = gameOver;
    }

    /**
     * @return Kartenbewegungen
     */
    public CardMoves moves() {
        return moves;
    }

    private void enqueue(Tween tween) {
        s.animator().enqueue(tween);
    }

    private void log(String text, ColorRGBA color) {
        s.hud().log(text, color);
    }

    private void log(String player, String text) {
        log(player + " " + text, s.color(player));
    }

    private String name(Card card) {
        return card == null ? "eine Karte" : card.getDisplayName();
    }

    private String who(String player) {
        return s.isMe(player) ? "Du" : player;
    }

    /**
     * Farbe einer Heldenklasse für Effekte.
     *
     * @param type Klasse
     * @return Farbe
     */
    public static ColorRGBA classColor(ClassType type) {
        return switch (type) {
            case MAGE -> new ColorRGBA(0.45f, 0.65f, 1f, 1f);
            case GUARD -> new ColorRGBA(1f, 0.82f, 0.35f, 1f);
            case FIGHTER -> new ColorRGBA(1f, 0.38f, 0.3f, 1f);
            case RANGER -> new ColorRGBA(0.45f, 0.9f, 0.4f, 1f);
            case THIEF -> new ColorRGBA(0.75f, 0.45f, 1f, 1f);
            case BARD -> new ColorRGBA(1f, 0.6f, 0.25f, 1f);
        };
    }

    // ------------------------------------------------------------------
    // Spielablauf
    // ------------------------------------------------------------------

    /**
     * Eröffnung: Kamera fährt an den Tisch, Monster werden aufgedeckt, Karten ausgeteilt.
     *
     * @param startPlayer beginnender Spieler
     */
    public void intro(String startPlayer) {
        List<Tween> deals = new ArrayList<>();
        List<CardNode> cards = new ArrayList<>();
        for (var seat : s.board().seats()) cards.addAll(seat.visibleCards());
        for (CardNode card : cards) {
            Transform target = card.getLocalTransform().clone();
            Transform start = target.clone();
            start.getTranslation().y += 30;
            start.setScale(0.01f);
            card.setLocalTransform(start);
            deals.add(Tweens.seq(Tweens.call(() -> s.sounds().play(Sfx.CARD_FLIP, 0.5f, 0.15f)),
                                 moves.fly(card, target, 0.5f, 0f, Easing.OUT_BACK)));
        }
        List<Card> myHand = new ArrayList<>(s.model().getHand());
        List<Tween> handDeal = new ArrayList<>();
        for (Card c : myHand) handDeal.add(moves.drawToHand(s.model().getMyName(), c, false, false));
        enqueue(Tweens.seq(
                Tweens.call(() -> {
                    s.rig().gameView(false);
                    s.sounds().play(Sfx.SHUFFLE);
                }),
                Tweens.delay(1.2f),
                Tweens.stagger(0.12f, deals),
                moves.revealOpenMonsters(),
                s.token().moveTo(s.seat(startPlayer)),
                Tweens.stagger(0.18f, handDeal),
                Tweens.delay(0.2f)));
        log("Das Spiel beginnt! " + startPlayer + " fängt an.", Theme.GOLD_LIGHT);
    }

    @Override
    public void onTurnStarted(TurnStartedEvent e) {
        enqueue(Tweens.seq(Tweens.call(() -> {
            s.board().setActive(e.player());
            refresh.run();
        }), Tweens.par(s.hud().turnSplash(e.player(), e.mine()), s.token().moveTo(s.seat(e.player())))));
        log(e.mine() ? "Du bist am Zug." : e.player() + " ist am Zug.", s.color(e.player()));
    }

    @Override
    public void onTurnEnded(TurnEndedEvent e) {
        log(who(e.player()), s.isMe(e.player()) ? "beendest deinen Zug." : "beendet den Zug.");
    }

    @Override
    public void onActionStateChanged(ActionStateChangedEvent e) {
        refresh.run();
    }

    @Override
    public void onNotice(NoticeEvent e) {
        s.hud().toast(e.text(), e.warning());
        if (!e.warning()) log(e.text(), Theme.MUTED);
    }

    @Override
    public void onGameOver(GameOverEvent e) {
        s.prompts().close();
        heroSelection.accept(null);
        enqueue(Tweens.call(() -> gameOver.accept(e)));
        log(e.winner() == null ? "Spiel beendet." : e.winner() + " gewinnt!", Theme.GOLD);
    }

    // ------------------------------------------------------------------
    // Kartenbewegungen
    // ------------------------------------------------------------------

    @Override
    public void onCardMoved(CardMovedEvent event) {
        BCCardMoved m = event.move();
        String owner = m.getOwner();
        Card card = m.getCard();
        switch (m.getCause()) {
            case DRAW -> {
                log(who(owner), s.isMe(owner) ? "ziehst " + name(card) + "." : "zieht eine Karte.");
                enqueue(moves.drawToHand(owner, card, false, false));
            }
            case EFFECT_DRAW -> {
                log(who(owner), s.isMe(owner) ? "erhältst " + name(card) + "." : "erhält eine Karte.");
                enqueue(Tweens.seq(Tweens.call(() -> s.sounds().play(Sfx.SPARKLE)),
                                   moves.drawToHand(owner, card, true, false)));
            }
            case EFFECT_DRAW_DISCARD -> {
                log(who(owner), (s.isMe(owner) ? "holst " : "holt ") + name(card) + " vom Ablagestapel.");
                enqueue(Tweens.seq(Tweens.call(() -> s.sounds().play(Sfx.MAGIC, 0.7f, 0.1f)),
                                   moves.drawToHand(owner, card, true, true)));
            }
            case PLAY -> {
                log(who(owner), (s.isMe(owner) ? "spielst " : "spielt ") + name(card) + " aus.");
                enqueue(moves.play(owner, card));
            }
            case PLAY_SUCCEEDED -> {
                if (card instanceof Hero hero && m.getTo() == CardPosition.PLAYER_GROUP) {
                    log(hero.getDisplayName() + " schließt sich " + (s.isMe(owner) ? "deiner" : owner + "s")
                        + " Gruppe an.", s.color(owner));
                    enqueue(moves.heroJoins(owner, hero));
                }
                else {
                    enqueue(spellFinished(owner));
                }
            }
            case PLAY_FAILED -> {
                log(name(card) + " wurde abgewehrt.", Theme.RED);
                enqueue(playFailed(owner));
            }
            case CHALLENGE -> {
                log(who(owner), (s.isMe(owner) ? "forderst" : "fordert") + " heraus!");
                enqueue(challengeCard(owner, card));
            }
            case EFFECT_DESTROY -> {
                log(name(card) + " von " + owner + " wird vom Blitz getroffen.", Theme.BLUE);
                enqueue(lightningDestroy(owner, (Hero) card));
            }
            case EFFECT_DESTROY_RANDOM -> {
                log("Einschlag! " + name(card) + " von " + owner + " ist zerstört.", Theme.RED);
                enqueue(mortarDestroy(owner, (Hero) card));
            }
            case MONSTER_PENALTY -> {
                log(who(owner), (s.isMe(owner) ? "opferst " : "opfert ") + name(card) + ".");
                enqueue(sacrifice(owner, (Hero) card));
            }
            case MONSTER_DEFEATED -> {
                log(who(owner), (s.isMe(owner) ? "besiegst " : "besiegt ") + name(card) + "!");
                enqueue(monsterDefeated(owner, (Monster) card));
            }
            default -> enqueue(genericMove(m));
        }
    }

    private Tween genericMove(BCCardMoved m) {
        if (m.getFrom() == CardPosition.PLAYER_HAND && m.getTo() == CardPosition.DISCARD_PILE)
            return moves.handToDiscard(m.getOwner(), m.getCard());
        if (m.getFrom() == CardPosition.PLAYER_GROUP && m.getCard() instanceof Hero hero)
            return moves.heroToDiscard(m.getOwner(), hero, false);
        return Tweens.delay(0.2f);
    }

    private Tween spellFinished(String owner) {
        return Tweens.defer(() -> {
            CardNode node = s.board().detachHover(s.seat(owner));
            if (node == null) return Tweens.NONE;
            return Tweens.seq(Tweens.call(() -> s.fx().twinkle(node.getWorldTranslation(), Theme.PURPLE)),
                              moves.toDiscard(node, 0.6f, false));
        });
    }

    private Tween playFailed(String owner) {
        return Tweens.defer(() -> {
            CardNode node = s.board().detachHover(s.seat(owner));
            if (node == null) return Tweens.NONE;
            Vector3f base = node.getLocalTranslation().clone();
            return Tweens.seq(
                    Tweens.call(() -> {
                        node.setHighlight(new ColorRGBA(1f, 0.15f, 0.1f, 1f), false);
                        s.sounds().play(Sfx.PIPE, 0.8f, 0.05f);
                    }),
                    Tweens.lerp(0.45f, t -> node.setLocalTranslation(
                            base.add(FastMath.sin(t * 40) * 0.6f * (1 - t), 0, 0))),
                    Tweens.call(() -> {
                        s.fx().embers(node.getWorldTranslation());
                        s.sounds().play(Sfx.BURN, 0.7f, 0.05f);
                    }),
                    Tweens.lerp(0.35f, t -> node.setBrightness(1 - 0.7f * t)),
                    moves.toDiscard(node, 0.55f, false),
                    Tweens.call(() -> node.setBrightness(1f)));
        });
    }

    private Tween challengeCard(String owner, Card card) {
        return Tweens.defer(() -> {
            CardNode node = moves.fromHand(owner, card);
            Vector3f mid = new Vector3f(0, 9, 10);
            Transform center = new Transform(mid, new Quaternion().fromAngleAxis(-0.9f, Vector3f.UNIT_X));
            center.setScale(1.3f);
            return Tweens.seq(
                    Tweens.call(() -> s.sounds().play(Sfx.WHOOSH)),
                    moves.fly(node, center, 0.55f, 4f, Easing.OUT_CUBIC),
                    s.fx().swords(new Vector3f(0, 2, 10)),
                    moves.toDiscard(node, 0.5f, false));
        });
    }

    private Tween lightningDestroy(String owner, Hero hero) {
        return Tweens.defer(() -> {
            CardNode node = s.board().findHero(s.seat(owner), hero);
            if (node == null) return moves.heroToDiscard(owner, hero, false);
            Vector3f pos = node.getWorldTranslation().clone();
            return Tweens.seq(
                    focusOn(owner, pos),
                    Tweens.delay(s.isMe(owner) ? 0f : 0.5f),
                    s.fx().lightning(pos, new ColorRGBA(0.6f, 0.8f, 1f, 1f)),
                    Tweens.call(() -> {
                        s.fx().embers(pos);
                        s.sounds().play(Sfx.BURN, 0.6f, 0.1f);
                    }),
                    Tweens.lerp(0.4f, t -> node.setBrightness(1 - 0.75f * t)),
                    moves.heroToDiscard(owner, hero, false),
                    Tweens.call(() -> node.setBrightness(1f)),
                    unfocus(owner));
        });
    }

    private Tween mortarDestroy(String owner, Hero hero) {
        return Tweens.defer(() -> {
            CardNode node = s.board().findHero(s.seat(owner), hero);
            if (node == null) return moves.heroToDiscard(owner, hero, true);
            Vector3f pos = node.getWorldTranslation().clone();
            return Tweens.seq(focusOn(owner, pos), Tweens.delay(s.isMe(owner) ? 0f : 0.4f), s.fx().mortar(pos),
                              moves.heroToDiscard(owner, hero, true), unfocus(owner));
        });
    }

    private Tween sacrifice(String owner, Hero hero) {
        return Tweens.defer(() -> {
            CardNode node = s.board().findHero(s.seat(owner), hero);
            if (node == null) return moves.heroToDiscard(owner, hero, false);
            Vector3f pos = node.getWorldTranslation().clone();
            return Tweens.seq(
                    s.fx().slash(pos, new ColorRGBA(1f, 0.75f, 0.6f, 1f)),
                    Tweens.call(() -> {
                        s.fx().smoke(pos.add(0, 1, 0), 3f, true);
                        s.rig().shake(0.4f, 0.2f);
                    }),
                    Tweens.lerp(0.35f, t -> node.setBrightness(1 - 0.6f * t)),
                    moves.heroToDiscard(owner, hero, false),
                    Tweens.call(() -> node.setBrightness(1f)));
        });
    }

    private Tween monsterDefeated(String owner, Monster monster) {
        return Tweens.defer(() -> {
            int index = s.board().openMonsterIndex(monster);
            CardNode node = index >= 0 ? s.board().openMonster(index) : null;
            Vector3f pos = node != null ? node.getWorldTranslation().clone() : s.board().layout().center();
            return Tweens.seq(
                    s.fx().slash(pos, Theme.GOLD_LIGHT),
                    Tweens.call(() -> {
                        s.sounds().play(Sfx.MONSTER_DEFEATED);
                        s.fx().sparks(pos.add(0, 2, 0), Theme.GOLD_LIGHT, 60, 16f);
                    }),
                    s.hud().stamp("Monster besiegt!", Theme.GOLD, 96),
                    moves.trophy(owner, monster),
                    Tweens.call(this::releaseFocus),
                    moves.revealOpenMonsters());
        });
    }

    @Override
    public void onCardsMoved(CardsMovedEvent event) {
        BCCardsMoved m = event.move();
        Map<String, List<Card>> byPlayer = m.getCardsByPlayer();
        switch (m.getCause()) {
            case EFFECT_DESTROY_PARTY -> {
                byPlayer.forEach((player, list) -> log("Luftangriff auf " + player + "! " + list.size()
                                                       + " Helden zerstört.", Theme.RED));
                enqueue(heliStrike(byPlayer));
            }
            case EFFECT_DESTROY_ALL -> {
                log("Bombe! Alle Helden auf dem Tisch werden zerstört.", Theme.RED);
                enqueue(bomb(byPlayer));
            }
            case EFFECT_DISCARD -> {
                log("Alle werfen eine Karte ab.", Theme.MUTED);
                List<Tween> parts = new ArrayList<>();
                byPlayer.forEach((player, list) -> list.forEach(c -> parts.add(moves.handToDiscard(player, c))));
                enqueue(Tweens.seq(Tweens.call(() -> s.sounds().play(Sfx.SHUFFLE)), Tweens.stagger(0.15f, parts)));
            }
            default -> {
                List<Tween> parts = new ArrayList<>();
                byPlayer.forEach((player, list) -> list.forEach(c -> {
                    if (m.getFrom() == CardPosition.PLAYER_GROUP && c instanceof Hero h)
                        parts.add(moves.heroToDiscard(player, h, false));
                    else if (m.getFrom() == CardPosition.PLAYER_HAND)
                        parts.add(moves.handToDiscard(player, c));
                }));
                enqueue(Tweens.stagger(0.12f, parts));
            }
        }
    }

    private Tween heliStrike(Map<String, List<Card>> byPlayer) {
        return Tweens.defer(() -> {
            List<Vector3f> targets = new ArrayList<>();
            List<Tween> blowAway = new ArrayList<>();
            Vector3f focus = s.board().layout().center();
            for (var entry : byPlayer.entrySet()) {
                int seat = s.seat(entry.getKey());
                focus = s.seatCenter(entry.getKey());
                for (Card c : entry.getValue()) {
                    if (!(c instanceof Hero hero)) continue;
                    CardNode node = s.board().findHero(seat, hero);
                    if (node != null) targets.add(node.getWorldTranslation().clone());
                    blowAway.add(moves.heroToDiscard(entry.getKey(), hero, true));
                }
            }
            return Tweens.seq(s.fx().heliStrike(targets, focus), Tweens.stagger(0.12f, blowAway));
        });
    }

    private Tween bomb(Map<String, List<Card>> byPlayer) {
        return Tweens.defer(() -> {
            List<Tween> blowAway = new ArrayList<>();
            byPlayer.forEach((player, list) -> {
                for (Card c : list) {
                    if (c instanceof Hero hero) blowAway.add(moves.heroToDiscard(player, hero, true));
                }
            });
            return Tweens.seq(s.fx().bomb(new Vector3f(0, 0, 4)), Tweens.stagger(0.07f, blowAway));
        });
    }

    @Override
    public void onMulligan(MulliganEvent event) {
        BCMulligan m = event.mulligan();
        String owner = m.getPlayerName();
        log(who(owner), s.isMe(owner) ? "tauschst deine Hand." : "tauscht die Hand.");
        List<Tween> discards = new ArrayList<>();
        for (Card c : m.getDiscarded()) discards.add(moves.handToDiscard(owner, c));
        List<Tween> draws = new ArrayList<>();
        if (s.isMe(owner)) {
            for (Card c : m.getDrawn()) draws.add(moves.drawToHand(owner, c, false, false));
        }
        else {
            for (int i = 0; i < m.getDrawnCount(); i++) draws.add(moves.drawToHand(owner, null, false, false));
        }
        enqueue(Tweens.seq(Tweens.call(() -> s.sounds().play(Sfx.SHUFFLE)), Tweens.stagger(0.08f, discards),
                           Tweens.delay(0.2f), Tweens.stagger(0.12f, draws)));
    }

    // ------------------------------------------------------------------
    // Effekte, Angriffe, Würfel
    // ------------------------------------------------------------------

    @Override
    public void onEffectActivated(EffectActivatedEvent e) {
        Card card = e.card();
        if (card instanceof Hero hero) {
            log(who(e.player()), (s.isMe(e.player()) ? "nutzt " : "nutzt ") + hero.getDisplayName() + ": "
                                 + hero.getEffect().describe());
            enqueue(heroGlow(e.player(), hero));
        }
        else if (card instanceof Spell spell) {
            log(who(e.player()), (s.isMe(e.player()) ? "wirkst " : "wirkt ") + spell.getDisplayName() + ": "
                                 + spell.getEffect().describe());
            enqueue(spellCast(e.player(), spell));
        }
    }

    private Tween heroGlow(String player, Hero hero) {
        return Tweens.defer(() -> {
            CardNode node = s.board().findHero(s.seat(player), hero);
            if (node == null) return Tweens.NONE;
            ColorRGBA color = classColor(hero.getClassType());
            Vector3f pos = node.getWorldTranslation().clone();
            return Tweens.seq(
                    Tweens.call(() -> {
                        s.sounds().play(Sfx.MAGIC, 1f, 0.05f);
                        node.setHighlight(color, true);
                    }),
                    Tweens.par(s.fx().magicCircle(pos, 16f, Theme.withAlpha(color, 0.9f), 1.3f),
                               s.fx().beam(pos, 26f, Theme.withAlpha(color, 0.7f), 1.1f),
                               Tweens.lerp(1.1f, t -> {
                                   float lift = Easing.PULSE.apply(t);
                                   node.getPivot().setLocalTranslation(0, lift * 2.5f, 0);
                               })),
                    Tweens.call(() -> node.setHighlight(null, false)));
        });
    }

    private Tween spellCast(String player, Spell spell) {
        return Tweens.defer(() -> {
            CardNode node = s.board().hover(s.seat(player));
            if (node == null) return Tweens.NONE;
            Vector3f pos = node.getWorldTranslation().clone();
            ColorRGBA purple = new ColorRGBA(0.75f, 0.45f, 1f, 1f);
            boolean dramatic = spell.getEffect() instanceof DestroyAllHeroes || spell.getEffect() instanceof DestroyParty
                               || spell.getEffect() instanceof DestroyRandomHeroes;
            return Tweens.seq(
                    Tweens.call(() -> {
                        s.sounds().play(Sfx.MAGIC);
                        node.setHighlight(purple, true);
                    }),
                    Tweens.par(s.fx().magicCircle(pos.subtract(0, pos.y - 0.1f, 0), 18f, purple, 1.2f),
                               Tweens.lerp(1.1f, Easing.OUT_CUBIC, t -> {
                                   float k = Easing.PULSE.apply(Math.min(1, t * 1.25f));
                                   node.getPivot().setLocalTranslation(0, k * 5f, 0);
                                   node.getPivot().setLocalScale(1 + k * 0.6f);
                               })),
                    dramatic ? s.hud().stamp(spell.getDisplayName(), purple, 84) : Tweens.NONE);
        });
    }

    @Override
    public void onMonsterAttacked(MonsterAttackedEvent e) {
        log(who(e.player()), (s.isMe(e.player()) ? "greifst " : "greift ") + e.monster().getDisplayName() + " an!");
        enqueue(Tweens.defer(() -> {
            int index = s.board().openMonsterIndex(e.monster());
            if (index < 0) return Tweens.NONE;
            CardNode node = s.board().openMonster(index);
            Vector3f pos = node.getWorldTranslation().clone();
            attackFocus = true;
            List<Tween> heroes = new ArrayList<>();
            int seat = s.seat(e.player());
            for (CardNode h : s.board().seat(seat).visibleCards()) {
                if (h.getCard() instanceof Hero) {
                    heroes.add(Tweens.seq(Tweens.call(() -> h.setHighlight(Theme.withAlpha(Theme.RED, 0.8f), true)),
                                          Tweens.delay(1.1f), Tweens.call(() -> h.setHighlight(null, false))));
                }
            }
            return Tweens.seq(
                    Tweens.call(() -> {
                        s.rig().focus(pos.add(0, 0, 8), 118f, 50f);
                        s.sounds().play(Sfx.MONSTER_ATTACK);
                        node.setHighlight(new ColorRGBA(1f, 0.25f, 0.15f, 1f), true);
                    }),
                    Tweens.par(Tweens.par(heroes),
                               Tweens.lerp(1.1f, t -> {
                                   float k = Easing.PULSE.apply(t);
                                   node.getPivot().setLocalTranslation(FastMath.sin(t * 50) * 0.35f * k, k * 3f, 0);
                               }),
                               s.fx().slash(pos, new ColorRGBA(1f, 0.85f, 0.6f, 1f))),
                    Tweens.call(() -> node.setHighlight(null, false)));
        }));
    }

    /**
     * Schwenkt die Kamera zu einem gegnerischen Platz, damit Effekte dort gut zu sehen sind.
     *
     * @param owner Spieler
     * @param pos   Ort des Effekts
     * @return Tween (sofort)
     */
    private Tween focusOn(String owner, Vector3f pos) {
        return Tweens.call(() -> {
            if (!s.isMe(owner)) s.rig().focus(pos.add(0, 0, 6), 92f, 50f);
        });
    }

    private Tween unfocus(String owner) {
        return Tweens.call(() -> {
            if (!s.isMe(owner)) s.rig().release();
        });
    }

    private void releaseFocus() {
        if (attackFocus) {
            attackFocus = false;
            s.rig().release();
        }
    }

    @Override
    public void onDiceRolled(DiceRolledEvent e) {
        DiceResult r = e.result();
        log(who(r.getPlayerName()), (s.isMe(r.getPlayerName()) ? "würfelst " : "würfelt ") + r.getTotal() + " ("
                                    + Texts.purpose(e.purpose()) + ", Ziel " + Texts.goal(e.threshold(), e.higherWins())
                                    + ")");
        ColorRGBA color = s.color(r.getPlayerName());
        enqueue(Tweens.seq(
                s.dicePanel().showSingle(r, e.purpose(), e.threshold(), e.higherWins(), color),
                s.dice().roll(s.board().layout().diceArea(), s.seatCenter(r.getPlayerName()), r.getDie1(), r.getDie2(),
                              color),
                s.dicePanel().reveal(r),
                Tweens.delay(0.35f)));
    }

    @Override
    public void onDiceModified(DiceModifiedEvent e) {
        log(who(e.modifier()), (s.isMe(e.modifier()) ? "spielst " : "spielt ") + e.modification().getDisplayName()
                               + " – Wurf jetzt " + e.result().getTotal() + ".");
        enqueue(Tweens.seq(modificationFlight(e.modifier(), e.modification()),
                           s.dicePanel().modified(e.result())));
    }

    private Tween modificationFlight(String modifier, Modification mod) {
        return Tweens.defer(() -> {
            CardNode node = moves.fromHand(modifier, mod);
            Vector3f area = s.board().layout().diceArea();
            Transform above = new Transform(area.add(0, 8, -2), new Quaternion().fromAngleAxis(-0.8f, Vector3f.UNIT_X));
            above.setScale(1.2f);
            ColorRGBA color = mod.getDelta() > 0 ? Theme.GREEN : Theme.RED;
            return Tweens.seq(
                    Tweens.call(() -> s.sounds().play(Sfx.WHOOSH)),
                    moves.fly(node, above, 0.5f, 3f, Easing.OUT_CUBIC),
                    Tweens.call(() -> {
                        node.setHighlight(color, false);
                        s.sounds().play(Sfx.SPARKLE);
                        s.fx().sparks(area.add(0, 3, 0), color, 30, 10f);
                    }),
                    Tweens.par(s.fx().floatingText(area.add(0, 6, 0), Texts.signed(mod.getDelta()), color, 80),
                               Tweens.seq(Tweens.delay(0.3f), moves.toDiscard(node, 0.5f, false))));
        });
    }

    @Override
    public void onRollResolved(RollResolvedEvent event) {
        BCRollResolved r = event.resolution();
        boolean ok = r.isSuccess();
        String text;
        if (r.getPurpose() == RollPurpose.MONSTER_ATTACK)
            text = ok ? "Treffer!" : "Daneben!";
        else
            text = ok ? "Erfolg!" : "Fehlschlag!";
        log(r.getPlayerName() + ": " + Texts.purpose(r.getPurpose()) + (ok ? " gelingt." : " misslingt."),
            ok ? Theme.GREEN : Theme.RED);
        enqueue(Tweens.seq(
                s.dicePanel().resolve(ok),
                Tweens.call(() -> {
                    if (ok)
                        s.sounds().play(Sfx.SUCCESS);
                    else
                        s.sounds().play(r.getPurpose() == RollPurpose.MONSTER_ATTACK ? Sfx.MONSTER_MISS : Sfx.FAIL);
                    if (ok && r.getTarget() instanceof Hero hero) {
                        CardNode node = s.board().findHero(s.seat(r.getPlayerName()), hero);
                        if (node != null) s.fx().twinkle(node.getWorldTranslation(), classColor(hero.getClassType()));
                    }
                }),
                s.hud().stamp(text, ok ? Theme.GREEN : Theme.RED, 110),
                Tweens.par(s.dicePanel().hide(), s.dice().clear()),
                Tweens.call(() -> {
                    if (!ok) releaseFocus();
                })));
    }

    @Override
    public void onChallengeRolled(ChallengeRolledEvent event) {
        BCChallengeRolled r = event.roll();
        log("Duell: " + r.getActivePlayer() + " " + r.getActiveResult().getTotal() + " gegen " + r.getChallenger() + " "
            + r.getChallengerResult().getTotal(), Theme.CREAM);
        Vector3f area = s.board().layout().diceArea();
        enqueue(Tweens.seq(
                s.dicePanel().showDouble(r.getActiveResult(), r.getChallengerResult(), s.color(r.getActivePlayer()),
                                         s.color(r.getChallenger())),
                Tweens.par(s.dice().roll(area.add(-7, 0, 0), s.seatCenter(r.getActivePlayer()),
                                         r.getActiveResult().getDie1(), r.getActiveResult().getDie2(),
                                         s.color(r.getActivePlayer())),
                           s.dice().roll(area.add(7, 0, 0), s.seatCenter(r.getChallenger()),
                                         r.getChallengerResult().getDie1(), r.getChallengerResult().getDie2(),
                                         s.color(r.getChallenger()))),
                s.dicePanel().revealDouble(r.getActiveResult(), r.getChallengerResult()),
                Tweens.delay(0.35f)));
    }

    @Override
    public void onChallengeModified(ChallengeModifiedEvent event) {
        BCChallengeModified m = event.modification();
        log(who(m.getModifier()), (s.isMe(m.getModifier()) ? "spielst " : "spielt ")
                                  + m.getModification().getDisplayName() + " auf " + m.getTargetPlayer() + ".");
        enqueue(Tweens.seq(modificationFlight(m.getModifier(), m.getModification()),
                           s.dicePanel().modifiedDouble(m.getActiveResult(), m.getChallengerResult(),
                                                        m.getTargetPlayer())));
    }

    @Override
    public void onChallengeResolved(ChallengeResolvedEvent event) {
        BCChallengeResolved r = event.resolution();
        String winner = r.isActiveWon() ? r.getActivePlayer() : r.getChallenger();
        log(winner + " gewinnt das Duell.", s.color(winner));
        enqueue(Tweens.seq(
                s.dicePanel().resolve(r.isActiveWon()),
                Tweens.call(() -> s.sounds().play(s.isMe(winner) ? Sfx.SUCCESS : Sfx.SWORDS)),
                s.hud().stamp(winner + " gewinnt!", s.color(winner), 92),
                Tweens.par(s.dicePanel().hide(), s.dice().clear())));
    }

    // ------------------------------------------------------------------
    // Anfragen
    // ------------------------------------------------------------------

    private List<Modification> myModifications() {
        List<Modification> mods = new ArrayList<>();
        for (Card c : s.model().getHand()) if (c instanceof Modification m) mods.add(m);
        return mods;
    }

    @Override
    public void onChallengeRequest(ChallengeRequestEvent e) {
        s.animator().whenIdle(() -> s.prompts().showChallenge(e.activePlayer(), e.card(),
                                                               yes -> s.logic().respondToChallenge(yes)));
    }

    @Override
    public void onModifySingleRequest(ModifySingleRequestEvent e) {
        s.animator().whenIdle(() -> s.prompts().showModifySingle(e.result(), e.purpose(), e.threshold(), e.higherWins(),
                                                                  myModifications(),
                                                                  mod -> s.logic().respondToModification(mod)));
    }

    @Override
    public void onModifyDoubleRequest(ModifyDoubleRequestEvent e) {
        s.animator().whenIdle(() -> s.prompts().showModifyDouble(e.activeResult(), e.challengerResult(),
                                                                  myModifications(),
                                                                  (mod, target) -> s.logic()
                                                                                    .respondToChallengeModification(mod, target)));
    }

    @Override
    public void onHeroSelectionRequest(HeroSelectionRequestEvent e) {
        s.animator().whenIdle(() -> {
            heroSelection.accept(e.candidates());
            s.prompts().showHeroChoice(e.candidates(), this::ownerOf, hero -> s.logic().chooseHero(hero));
        });
    }

    private String ownerOf(Hero hero) {
        for (var p : s.model().getPlayers()) {
            for (Hero h : p.getGroup()) if (hero.equals(h)) return p.getName();
        }
        return "";
    }

    @Override
    public void onPlayerSelectionRequest(PlayerSelectionRequestEvent e) {
        s.animator().whenIdle(() -> s.prompts().showPlayerChoice(
                e.candidates(),
                name -> s.model().getPlayer(name).map(p -> p.getHeroCount() + " Helden").orElse(""),
                name -> s.logic().choosePlayer(name)));
    }

    @Override
    public void onRequestClosed(RequestClosedEvent e) {
        s.prompts().close();
        heroSelection.accept(null);
        refresh.run();
    }
}
