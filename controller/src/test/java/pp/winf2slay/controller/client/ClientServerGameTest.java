package pp.winf2slay.controller.client;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pp.winf2slay.controller.client.event.GameEventListener;
import pp.winf2slay.controller.client.event.GameStartedEvent;
import pp.winf2slay.controller.client.event.LobbyChangedEvent;
import pp.winf2slay.controller.client.event.NameConfirmedEvent;
import pp.winf2slay.controller.message.BotLevel;
import pp.winf2slay.controller.server.ServerGameLogic;
import pp.winf2slay.controller.server.VirtualScheduler;
import pp.winf2slay.controller.server.state.GameOverState;
import pp.winf2slay.model.Game;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Zwei „menschliche“ Clients (über die Client-Logik) und Bots spielen eine
 * komplette Partie gegen den Server – ohne Netzwerk, aber mit genau dem
 * Nachrichtenfluss der Anwendung.
 */
class ClientServerGameTest {

    @ParameterizedTest(name = "Seed {0}")
    @ValueSource(longs = {1, 7, 21, 1234})
    void humansAndBotsFinishAGame(long seed) {
        VirtualScheduler scheduler = new VirtualScheduler();
        Map<Integer, ClientGameLogic> clients = new HashMap<>();
        // Server → Client: asynchron wie über das Netzwerk
        ServerGameLogic server = new ServerGameLogic(
                (id, msg) -> scheduler.schedule(() -> clients.get(id).receive(msg), 1), scheduler, new Game(seed));

        Map<Integer, AutoPlayer> players = new HashMap<>();
        boolean[] started = new boolean[2];
        String[] names = new String[2];
        for (int id = 0; id < 2; id++) {
            int clientId = id;
            ClientGameLogic client = new ClientGameLogic(msg -> scheduler.schedule(() -> server.receive(msg, clientId), 1));
            clients.put(id, client);
            players.put(id, new AutoPlayer(client, task -> scheduler.schedule(task, 5)));
            client.getEvents().addListener(new GameEventListener() {
                @Override
                public void onNameConfirmed(NameConfirmedEvent event) {
                    names[clientId] = event.name();
                }

                @Override
                public void onGameStarted(GameStartedEvent event) {
                    started[clientId] = true;
                }
            });
            assertTrue(server.connectionAdded(id));
            client.connected();
            client.joinLobby("Mensch");
        }
        scheduler.runUntil(() -> names[1] != null, 1000);
        assertEquals("Mensch", names[0]);
        assertEquals("Mensch 2", names[1], "doppelte Namen müssen eindeutig gemacht werden");

        boolean[] lobbyHasBots = {false};
        clients.get(0).getEvents().addListener(new GameEventListener() {
            @Override
            public void onLobbyChanged(LobbyChangedEvent event) {
                lobbyHasBots[0] = event.host() && event.entries().size() == 4;
            }
        });
        clients.get(1).addBot(BotLevel.NORMAL); // kein Host → wird ignoriert
        clients.get(0).addBot(BotLevel.NORMAL);
        clients.get(0).addBot(BotLevel.HARD);
        scheduler.runUntil(() -> lobbyHasBots[0], 1000);
        assertTrue(lobbyHasBots[0], "Host sollte zwei Bots hinzufügen können");

        clients.get(0).startGame();
        boolean finished = scheduler.runUntil(() -> server.getState() instanceof GameOverState
                                                    && players.values().stream().allMatch(p -> p.getGameOver() != null),
                                              2_000_000);
        assertTrue(started[0] && started[1], "Beide Clients müssen den Spielstart erhalten");
        assertTrue(finished, "Partie wurde nicht beendet");
        String winner = players.get(0).getGameOver().winner();
        assertNotNull(winner);
        assertEquals(winner, players.get(1).getGameOver().winner());
        assertTrue(players.get(0).getActions() > 0, "Client 0 hat nie gehandelt");
        assertTrue(players.get(1).getActions() > 0, "Client 1 hat nie gehandelt");
        assertTrue(players.values().stream().allMatch(p -> p.getNotices().stream()
                                                            .noneMatch(n -> n.contains("Aktionspunkte"))),
                   "Die Client-Prüfungen müssen zu den Serverregeln passen");
    }
}
