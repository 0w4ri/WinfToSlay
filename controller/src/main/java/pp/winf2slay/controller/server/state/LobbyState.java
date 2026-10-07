package pp.winf2slay.controller.server.state;

import pp.winf2slay.controller.message.client.CMAddBot;
import pp.winf2slay.controller.message.client.CMJoinLobby;
import pp.winf2slay.controller.message.client.CMLeaveLobby;
import pp.winf2slay.controller.message.client.CMRemoveBot;
import pp.winf2slay.controller.message.client.CMStartGame;
import pp.winf2slay.controller.message.server.BCGameStarted;
import pp.winf2slay.controller.message.server.SMNotice;
import pp.winf2slay.controller.message.server.SMWelcome;
import pp.winf2slay.controller.server.ServerGameLogic;
import pp.winf2slay.controller.server.ServerState;
import pp.winf2slay.controller.server.TestSetup;
import pp.winf2slay.model.Game;
import pp.winf2slay.model.Player;
import pp.winf2slay.model.card.Leader;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Optional;

/**
 * Lobby: Spieler treten bei, der Spielleiter fügt Bots hinzu und startet die Partie.
 */
public class LobbyState extends ServerState {

    /**
     * Maximale Länge eines Spielernamens.
     */
    public static final int MAX_NAME_LENGTH = 16;

    /**
     * Mindestanzahl Spieler (Menschen + Bots) für den Spielstart.
     */
    public static final int MIN_PLAYERS = 2;

    /**
     * @param logic Serverlogik
     */
    public LobbyState(ServerGameLogic logic) {
        super(logic);
    }

    @Override
    public boolean playerConnected(int id) {
        if (logic.getPlayers().size() >= Game.MAX_PLAYERS) {
            LOGGER.log(Level.INFO, "Lobby ist voll – Verbindung {0} abgewiesen", id);
            return false;
        }
        logic.addHuman(id);
        return true;
    }

    @Override
    public void playerDisconnected(int id) {
        logic.findPlayer(id).ifPresent(p -> {
            logic.removePlayer(p);
            logic.broadcastLobby();
        });
    }

    @Override
    public void received(CMJoinLobby msg, int from) {
        Optional<Player> player = logic.findPlayer(from);
        if (player.isEmpty()) {
            unexpected(msg, from);
            return;
        }
        String wanted = sanitize(msg.getName(), player.get().getName());
        player.get().setName("#" + from); // eigenen Namen bei der Eindeutigkeitsprüfung ignorieren
        String finalName = logic.uniqueName(wanted);
        player.get().setName(finalName);
        logic.send(player.get(), new SMWelcome(finalName));
        logic.broadcastLobby();
    }

    @Override
    public void received(CMLeaveLobby msg, int from) {
        playerDisconnected(from);
    }

    @Override
    public void received(CMAddBot msg, int from) {
        if (!logic.isHost(from)) {
            unexpected(msg, from);
            return;
        }
        if (logic.getPlayers().size() >= Game.MAX_PLAYERS) {
            logic.findPlayer(from).ifPresent(p -> logic.send(p, new SMNotice("Es sind bereits " + Game.MAX_PLAYERS + " Spieler in der Lobby.")));
            return;
        }
        logic.addBot(msg.getLevel());
        logic.broadcastLobby();
    }

    @Override
    public void received(CMRemoveBot msg, int from) {
        if (!logic.isHost(from)) {
            unexpected(msg, from);
            return;
        }
        logic.findPlayer(msg.getName()).filter(Player::isBot).ifPresent(bot -> {
            logic.removePlayer(bot);
            logic.broadcastLobby();
        });
    }

    @Override
    public void received(CMStartGame msg, int from) {
        if (!logic.isHost(from)) {
            unexpected(msg, from);
            return;
        }
        if (logic.getPlayers().size() < MIN_PLAYERS) {
            logic.findPlayer(from).ifPresent(p -> logic.send(p, new SMNotice("Für ein Spiel werden mindestens " + MIN_PLAYERS + " Spieler benötigt – füge einen Bot hinzu.")));
            return;
        }
        startGame();
    }

    /**
     * Verteilt Anführer und Startkarten und beginnt den ersten Zug.
     */
    private void startGame() {
        Game game = logic.getGame();
        for (Player p : logic.getPlayers()) {
            Leader leader = game.drawLeader().orElseThrow(() -> new IllegalStateException("Keine Anführer mehr"));
            p.assignLeader(leader);
            for (int i = 0; i < Game.HAND_SIZE; i++) {
                game.drawSupportCard(p);
            }
        }
        TestSetup.apply(game, logic.getPlayers());
        int start = 0;
        logic.beginTurn(start);
        TurnState turnState = new TurnState(logic);
        logic.setState(turnState);
        List<String> seating = logic.getPlayers().stream().map(Player::getName).toList();
        LOGGER.log(Level.INFO, "Spiel startet mit {0}", seating);
        logic.broadcast(new BCGameStarted(seating, logic.getActivePlayer().getName()), () -> turnState.startTurn(start));
    }

    /**
     * Bereinigt einen Wunschnamen.
     *
     * @param wanted   Wunschname
     * @param fallback Name, falls der Wunschname leer ist
     * @return bereinigter Name
     */
    public static String sanitize(String wanted, String fallback) {
        String name = wanted == null ? "" : wanted.strip().replaceAll("\\s+", " ");
        StringBuilder clean = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (isAllowedNameChar(c)) clean.append(c);
        }
        name = clean.toString().strip();
        if (name.length() > MAX_NAME_LENGTH) name = name.substring(0, MAX_NAME_LENGTH).strip();
        return name.isEmpty() ? fallback : name;
    }

    /**
     * Erlaubte Zeichen in Spielernamen – dieselben im Client (Eingabefeld) und auf dem
     * Server. Die Schriften der Oberfläche können alle darstellen.
     *
     * @param c Zeichen
     * @return {@code true}, wenn das Zeichen erlaubt ist
     */
    public static boolean isAllowedNameChar(char c) {
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || "ÄÖÜäöüß _.-".indexOf(c) >= 0;
    }
}
