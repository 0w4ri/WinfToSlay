package pp.winf2slay.controller.server.state;

import pp.winf2slay.controller.message.server.SMGameOver;
import pp.winf2slay.controller.server.ServerGameLogic;
import pp.winf2slay.controller.server.ServerState;

import java.lang.System.Logger.Level;

/**
 * Spielende: Alle Spieler werden informiert; weitere Nachrichten werden ignoriert.
 */
public class GameOverState extends ServerState {

    private final String winner;
    private final String reason;

    /**
     * @param logic  Serverlogik
     * @param winner Gewinner oder {@code null}, wenn das Spiel abgebrochen wurde
     * @param reason Text für die Anzeige
     */
    public GameOverState(ServerGameLogic logic, String winner, String reason) {
        super(logic);
        this.winner = winner;
        this.reason = reason;
    }

    @Override
    public void entry() {
        LOGGER.log(Level.INFO, "Spielende: {0}", reason);
        logic.sendToAll(new SMGameOver(winner, reason));
        logic.notifyGameFinished();
    }

    /**
     * @return Gewinner oder {@code null}
     */
    public String getWinner() {
        return winner;
    }
}
