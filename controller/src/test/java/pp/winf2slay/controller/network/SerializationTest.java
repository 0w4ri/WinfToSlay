package pp.winf2slay.controller.network;

import com.jme3.network.serializing.Serializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pp.winf2slay.controller.message.BotLevel;
import pp.winf2slay.controller.message.CardPosition;
import pp.winf2slay.controller.message.LobbyEntry;
import pp.winf2slay.controller.message.MoveCause;
import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.controller.message.client.CMAddBot;
import pp.winf2slay.controller.message.client.CMChallengeResponse;
import pp.winf2slay.controller.message.client.CMModifyDoubleResponse;
import pp.winf2slay.controller.message.client.CMPlayCard;
import pp.winf2slay.controller.message.server.BCCardMoved;
import pp.winf2slay.controller.message.server.BCCardsMoved;
import pp.winf2slay.controller.message.server.BCChallengeRolled;
import pp.winf2slay.controller.message.server.BCMulligan;
import pp.winf2slay.controller.message.server.BroadcastMessage;
import pp.winf2slay.controller.message.server.SMHeroSelectionRequest;
import pp.winf2slay.controller.message.server.SMLobbyUpdate;
import pp.winf2slay.controller.server.ServerGameLogic;
import pp.winf2slay.controller.server.VirtualScheduler;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Card;
import pp.winf2slay.model.card.Hero;
import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.deck.CardCatalog;
import pp.winf2slay.model.die.DiceResult;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Prüft, dass alle Nachrichten (inklusive Karten, Effekten und Spielzustand)
 * über die jME-Serialisierung übertragen werden können.
 */
class SerializationTest {

    @BeforeAll
    static void register() {
        NetworkRegistry.registerAll();
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T object) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
        Serializer.writeClassAndObject(buffer, object);
        buffer.flip();
        return (T) Serializer.readClassAndObject(buffer);
    }

    @Test
    void allCardsSurviveSerialization() throws IOException {
        for (Card card : CardCatalog.supportCards()) {
            Card copy = roundTrip(card);
            assertEquals(card, copy);
            assertEquals(card.getDisplayName(), copy.getDisplayName());
            if (card instanceof Hero hero) {
                Hero h = (Hero) copy;
                assertEquals(hero.getThreshold(), h.getThreshold());
                assertEquals(hero.getEffect().describe(), h.getEffect().describe());
                assertEquals(hero.getClassType(), h.getClassType());
            }
        }
        for (Card card : CardCatalog.monsters()) assertEquals(card, roundTrip(card));
        for (Card card : CardCatalog.leaders()) assertEquals(card, roundTrip(card));
    }

    @Test
    void clientMessages() throws IOException {
        assertEquals(BotLevel.HARD, roundTrip(new CMAddBot(BotLevel.HARD)).getLevel());
        Hero hero = CardCatalog.heroes().get(3);
        assertEquals(hero, roundTrip(new CMPlayCard(hero)).getCard());
        assertNull(roundTrip(CMChallengeResponse.decline()).getChallenge());
        CMModifyDoubleResponse mod = roundTrip(new CMModifyDoubleResponse(new Modification(-4), "Anna"));
        assertEquals(-4, mod.getModification().getDelta());
        assertEquals("Anna", mod.getTargetPlayer());
    }

    @Test
    void serverMessagesWithPersonalizedSync() throws IOException {
        ServerGameLogic logic = new ServerGameLogic((id, m) -> {}, new VirtualScheduler(), new Game(3));
        logic.connectionAdded(0);
        logic.connectionAdded(1);
        Player anna = logic.findPlayer(0).orElseThrow();
        Player bert = logic.findPlayer(1).orElseThrow();
        Card drawn = logic.getGame().drawSupportCard(anna).orElseThrow();

        BroadcastMessage msg = new BCCardMoved(anna.getName(), anna.getName(), drawn, CardPosition.SUPPORT_DECK,
                                               CardPosition.PLAYER_HAND, MoveCause.DRAW);
        BCCardMoved forAnna = (BCCardMoved) roundTrip(msg.personalizedFor(7, anna.getName(), logic.createSync(anna)));
        BCCardMoved forBert = (BCCardMoved) roundTrip(msg.personalizedFor(7, bert.getName(), logic.createSync(bert)));
        assertEquals(drawn, forAnna.getCard());
        assertNull(forBert.getCard(), "gezogene Karten dürfen Gegner nicht sehen");
        assertEquals(7, forBert.getSyncId());
        assertEquals(1, forAnna.getSync().getHand().size());
        assertEquals(0, forBert.getSync().getHand().size());
        assertEquals(1, forBert.getSync().getPlayers().getFirst().getHandCount());
        assertNull(forBert.getSync().getPlayers().getFirst().getHover());

        BCMulligan mulligan = new BCMulligan(anna.getName(), List.of(drawn), List.of(drawn, drawn));
        BCMulligan forBertMulligan = (BCMulligan) roundTrip(mulligan.personalizedFor(8, bert.getName(),
                                                                                     logic.createSync(bert)));
        assertEquals(0, forBertMulligan.getDrawn().size());
        assertEquals(2, forBertMulligan.getDrawnCount());

        Hero hero = CardCatalog.heroes().getFirst();
        BCCardsMoved many = BCCardsMoved.of(Map.of(anna.getName(), List.of(hero)), CardPosition.PLAYER_GROUP,
                                            CardPosition.DISCARD_PILE, MoveCause.EFFECT_DESTROY_ALL);
        BCCardsMoved copy = (BCCardsMoved) roundTrip(many.personalizedFor(9, anna.getName(), logic.createSync(anna)));
        assertEquals(List.of(hero), copy.getCardsByPlayer().get(anna.getName()));

        DiceResult a = new DiceResult("A", 3, 4, 1, -2);
        BCChallengeRolled rolled = (BCChallengeRolled) roundTrip(
                new BCChallengeRolled("A", a, "B", a).personalizedFor(10, anna.getName(), logic.createSync(anna)));
        assertEquals(6, rolled.getActiveResult().getTotal());

        assertEquals(List.of(hero), roundTrip(new SMHeroSelectionRequest(List.of(hero))).getCandidates());
        SMLobbyUpdate lobby = roundTrip(new SMLobbyUpdate(List.of(new LobbyEntry("Bot Zuse", true, false, BotLevel.EASY))));
        assertEquals(BotLevel.EASY, lobby.getEntries().getFirst().getLevel());
        assertEquals(RollPurpose.CHALLENGE, RollPurpose.valueOf("CHALLENGE"));
    }
}
