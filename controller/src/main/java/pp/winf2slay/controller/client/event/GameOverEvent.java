package pp.winf2slay.controller.client.event;

/**
 * Die Partie ist beendet.
 *
 * @param winner Gewinner oder {@code null} bei Abbruch
 * @param reason Begründung für die Anzeige
 * @param won {@code true}, wenn dieser Client gewonnen hat
 */
public record GameOverEvent(String winner, String reason, boolean won) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onGameOver(this);
    }
}
