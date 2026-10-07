package pp.winf2slay.controller.server;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pp.winf2slay.controller.message.BotLevel;
import pp.winf2slay.controller.message.client.CMAddBot;
import pp.winf2slay.controller.message.client.CMJoinLobby;
import pp.winf2slay.controller.message.client.CMStartGame;
import pp.winf2slay.controller.message.server.BCGameStarted;
import pp.winf2slay.controller.message.server.ServerMessage;
import pp.winf2slay.controller.server.state.GameOverState;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lässt komplette Partien nur mit Bots laufen. Damit werden Serverlogik,
 * Abläufe (Herausforderungen, Würfe, Effekte) und Bots gemeinsam geprüft.
 *
 * <p>Mit {@code ./gradlew :controller:test -i} werden je Partie die Anzahl
 * der Züge und die gewählten Aktionen ausgegeben (Zeilen „STAT“).</p>
 */
class BotSimulationTest {

    private static final int MAX_TASKS = 500_000;

    /**
     * Merkt sich die letzten Log-Einträge (für hängende Partien) und zählt Aktionen.
     */
    private static final class RecordingHandler extends Handler {
        private final Deque<String> lastLines = new ArrayDeque<>();
        private final Map<String, Integer> actions = new TreeMap<>();
        private int turns;

        @Override
        public void publish(LogRecord record) {
            String msg = record.getParameters() == null
                         ? record.getMessage()
                         : MessageFormat.format(record.getMessage().replace("'", "''"), record.getParameters());
            if (msg.endsWith("ist am Zug")) turns++;
            if (msg.startsWith("received CM") && !msg.startsWith("received CMAnimationsDone")) {
                int end = msg.indexOf('[') > 0 ? msg.indexOf('[') : msg.indexOf(' ', 10);
                actions.merge(msg.substring("received ".length(), end), 1, Integer::sum);
            }
            lastLines.addLast(record.getLevel() + " " + msg);
            if (lastLines.size() > 80) lastLines.removeFirst();
        }

        @Override
        public void flush() {
            // nichts zu tun
        }

        @Override
        public void close() {
            // nichts zu tun
        }
    }

    static Stream<Arguments> games() {
        List<Arguments> args = new ArrayList<>();
        long[] seeds = {1, 2, 3, 13, 42, 99, 123, 2024};
        int[] botCounts = {1, 2, 3, 5};
        BotLevel[] levels = BotLevel.values();
        int i = 0;
        for (long seed : seeds) {
            for (int bots : botCounts) {
                args.add(Arguments.of(seed, bots, levels[i++ % levels.length]));
            }
        }
        return args.stream();
    }

    @ParameterizedTest(name = "Seed {0}, {1} Bots, {2}")
    @MethodSource("games")
    void botsPlayACompleteGame(long seed, int botCount, BotLevel level) {
        Logger root = Logger.getLogger("pp.winf2slay");
        RecordingHandler recorder = new RecordingHandler();
        recorder.setLevel(Level.ALL);
        Level previous = root.getLevel();
        root.setLevel(Level.FINE);
        root.addHandler(recorder);
        try {
            simulate(seed, botCount, level, recorder);
        }
        finally {
            root.removeHandler(recorder);
            root.setLevel(previous);
        }
    }

    private void simulate(long seed, int botCount, BotLevel level, RecordingHandler recorder) {
        List<ServerMessage> toHost = new ArrayList<>();
        VirtualScheduler scheduler = new VirtualScheduler();
        ServerGameLogic logic = new ServerGameLogic((id, msg) -> toHost.add(msg), scheduler, new Game(seed));

        // Der "Host" tritt bei, füllt die Lobby mit Bots und startet ...
        assertTrue(logic.connectionAdded(0));
        logic.receive(new CMJoinLobby("Host"), 0);
        for (int i = 0; i < botCount; i++) logic.receive(new CMAddBot(level), 0);
        logic.receive(new CMStartGame(), 0);
        // ... und überlässt seinen Platz sofort einem Bot (prüft dabei auch die Übernahme).
        Player host = logic.findPlayer(0).orElseThrow();
        logic.replaceByBot(host);

        boolean finished = scheduler.runUntil(() -> logic.getState() instanceof GameOverState, MAX_TASKS);
        if (!finished) {
            recorder.lastLines.forEach(System.out::println);
            for (Player p : logic.getPlayers())
                System.out.println(p + " Hand=" + p.getHand().size() + " Gruppe=" + p.getGroup().getHeroes()
                                   + " Monster=" + p.getMonsters().getMonsters() + " Klassen=" + p.getClassTypes());
        }
        assertTrue(finished, "Spiel nach " + scheduler.getExecuted() + " Aufgaben nicht beendet");

        GameOverState over = assertInstanceOf(GameOverState.class, logic.getState());
        System.out.printf("STAT seed=%d bots=%d level=%s turns=%d winner=%s %s%n", seed, botCount, level,
                          recorder.turns, over.getWinner(), recorder.actions);
        assertNotNull(over.getWinner(), "es muss einen Gewinner geben");
        Player winner = logic.findPlayer(over.getWinner()).orElseThrow();
        assertTrue(logic.getGame().hasWon(winner), "Gewinner erfüllt keine Siegbedingung");
        assertTrue(toHost.stream().anyMatch(m -> m instanceof BCGameStarted),
                   "Host wurde nicht über den Spielstart informiert");

        // Keine Karte darf verloren gehen: 91 Unterstützungskarten insgesamt.
        Game game = logic.getGame();
        int cards = game.getSupportDeck().size() + game.getDiscardPile().size();
        for (Player p : game.getPlayers()) {
            cards += p.getHand().size() + p.getGroup().size() + (p.getHover().isEmpty() ? 0 : 1);
        }
        assertEquals(91, cards, "Kartenanzahl stimmt nicht");
    }
}
