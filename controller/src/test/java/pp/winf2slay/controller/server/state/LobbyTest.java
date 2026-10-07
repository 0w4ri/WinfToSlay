package pp.winf2slay.controller.server.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pp.winf2slay.controller.client.ClientGameLogic;
import pp.winf2slay.controller.client.event.GameEventListener;
import pp.winf2slay.controller.client.event.NoticeEvent;
import pp.winf2slay.controller.message.BotLevel;
import pp.winf2slay.controller.message.client.CMAddBot;
import pp.winf2slay.controller.message.client.CMRemoveBot;
import pp.winf2slay.controller.message.client.CMStartGame;
import pp.winf2slay.controller.server.ServerGameLogic;
import pp.winf2slay.controller.server.VirtualScheduler;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regeln der Lobby: Spielleitung, Bots, Mindest- und Höchstzahl der Spieler und
 * der Wechsel eines Menschen zum Bot, wenn er ein laufendes Spiel verlässt.
 */
class LobbyTest {

    private static final int HOST = 0;
    private static final int GUEST = 1;

    private final VirtualScheduler scheduler = new VirtualScheduler();
    private final Map<Integer, ClientGameLogic> clients = new HashMap<>();
    private final Map<Integer, List<String>> notices = new HashMap<>();
    private ServerGameLogic server;

    @BeforeEach
    void setUp() {
        server = new ServerGameLogic((id, msg) -> {
            ClientGameLogic client = clients.get(id);
            if (client != null) scheduler.schedule(() -> client.receive(msg), 1);
        }, scheduler, new Game(5));
    }

    private ClientGameLogic connect(int id, String name) {
        ClientGameLogic client = new ClientGameLogic(msg -> scheduler.schedule(() -> server.receive(msg, id), 1));
        clients.put(id, client);
        List<String> received = new ArrayList<>();
        notices.put(id, received);
        client.getEvents().addListener(new GameEventListener() {
            @Override
            public void onNotice(NoticeEvent event) {
                received.add(event.text());
            }
        });
        assertTrue(server.connectionAdded(id), "Verbindung sollte angenommen werden");
        client.connected();
        client.joinLobby(name);
        settle();
        return client;
    }

    /**
     * Arbeitet alle anstehenden Nachrichten ab (in der Lobby gibt es keine Endlosschleifen).
     */
    private void settle() {
        scheduler.runUntil(() -> false, 10_000);
    }

    @Test
    void startNeedsAtLeastTwoPlayers() {
        ClientGameLogic host = connect(HOST, "Anna");
        host.startGame();
        settle();
        assertInstanceOf(LobbyState.class, server.getState());
        assertTrue(notices.get(HOST).stream().anyMatch(n -> n.contains("mindestens")),
                   "Der Spielleiter sollte erfahren, warum das Spiel nicht startet");

        host.addBot(BotLevel.EASY);
        settle();
        host.startGame();
        scheduler.runUntil(() -> server.getState() instanceof TurnState, 1_000);
        assertInstanceOf(TurnState.class, server.getState());
    }

    @Test
    void botsFillTheLobbyUpToTheMaximum() {
        ClientGameLogic host = connect(HOST, "Anna");
        for (int i = 0; i < Game.MAX_PLAYERS + 2; i++) {
            host.addBot(BotLevel.NORMAL);
            settle();
        }
        assertEquals(Game.MAX_PLAYERS, server.getPlayers().size());
        assertEquals(Game.MAX_PLAYERS - 1, server.getPlayers().stream().filter(Player::isBot).count());
        assertEquals(Game.MAX_PLAYERS, server.getPlayers().stream().map(Player::getName).distinct().count(),
                     "Alle Spieler brauchen eindeutige Namen");
        assertFalse(server.connectionAdded(42), "Eine volle Lobby nimmt niemanden mehr auf");
    }

    @Test
    void onlyTheHostManagesBotsAndStartsTheGame() {
        ClientGameLogic host = connect(HOST, "Anna");
        connect(GUEST, "Ben");
        server.receive(new CMAddBot(BotLevel.HARD), GUEST);
        settle();
        assertEquals(2, server.getPlayers().size(), "Mitspieler dürfen keine Bots hinzufügen");

        host.addBot(BotLevel.HARD);
        settle();
        String bot = server.getPlayers().stream().filter(Player::isBot).findFirst().orElseThrow().getName();
        server.receive(new CMRemoveBot(bot), GUEST);
        server.receive(new CMStartGame(), GUEST);
        settle();
        assertEquals(3, server.getPlayers().size(), "Mitspieler dürfen keine Bots entfernen");
        assertInstanceOf(LobbyState.class, server.getState(), "Mitspieler dürfen das Spiel nicht starten");

        host.removeBot(bot);
        settle();
        assertEquals(2, server.getPlayers().size());
        assertTrue(server.getPlayers().stream().noneMatch(Player::isBot));
    }

    @Test
    void hostRolePassesOnWhenTheHostLeaves() {
        connect(HOST, "Anna");
        ClientGameLogic guest = connect(GUEST, "Ben");
        assertFalse(guest.getModel().isHost());

        server.connectionRemoved(HOST);
        settle();
        assertTrue(server.isHost(GUEST));
        assertTrue(guest.getModel().isHost(), "Der Client muss erfahren, dass er jetzt Spielleiter ist");
        guest.addBot(BotLevel.NORMAL);
        settle();
        assertEquals(2, server.getPlayers().size());
    }

    @Test
    void leavingARunningGameHandsTheSeatToABot() {
        ClientGameLogic host = connect(HOST, "Anna");
        connect(GUEST, "Ben");
        host.startGame();
        scheduler.runUntil(() -> server.getState() instanceof TurnState, 1_000);
        assertInstanceOf(TurnState.class, server.getState());
        assertFalse(server.connectionAdded(7), "Laufende Spiele nehmen keine neuen Spieler auf");

        server.connectionRemoved(GUEST);
        Player ben = server.findPlayer("Ben").orElseThrow();
        assertTrue(ben.isBot(), "Ein Bot sollte den Platz übernehmen");
        scheduler.runUntil(() -> notices.get(HOST).stream().anyMatch(n -> n.contains("Bot übernimmt")), 1_000);
        assertTrue(notices.get(HOST).stream().anyMatch(n -> n.contains("Ben") && n.contains("Bot übernimmt")));
        assertInstanceOf(TurnState.class, server.getState(), "Das Spiel läuft weiter");
    }

    @Test
    void namesAreCleanedUp() {
        assertEquals("Anna Lena", LobbyState.sanitize("  Anna   Lena ", "x"));
        assertEquals("Fallback", LobbyState.sanitize("<>{}", "Fallback"));
        assertEquals("Fallback", LobbyState.sanitize(null, "Fallback"));
        assertEquals(LobbyState.MAX_NAME_LENGTH, LobbyState.sanitize("A".repeat(40), "x").length());
    }
}
