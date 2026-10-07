package pp.winf2slay.view.game;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import pp.winf2slay.controller.client.ClientGameLogic;
import pp.winf2slay.controller.client.ClientGameState;
import pp.winf2slay.view.anim.Animator;
import pp.winf2slay.view.audio.SoundManager;
import pp.winf2slay.view.fx.Fx;
import pp.winf2slay.view.game.board.BoardView;
import pp.winf2slay.view.game.card.HandView;
import pp.winf2slay.view.game.hud.DicePanel;
import pp.winf2slay.view.game.hud.Hud;
import pp.winf2slay.view.game.hud.PromptView;
import pp.winf2slay.view.ui.Theme;

/**
 * Bündelt alle Bausteine der Spielansicht, damit Animationen und Effekte darauf
 * zugreifen können.
 *
 * @param logic     Client-Logik
 * @param board     Spieltisch
 * @param hand      eigene Hand
 * @param hud       Oberfläche
 * @param dicePanel Würfelanzeige
 * @param dice      3D-Würfel
 * @param prompts   Anfragen
 * @param fx        Effekte
 * @param rig       Kamera
 * @param sounds    Sounds
 * @param animator  Animationen
 * @param bridge    Bildschirm ↔ Szene
 * @param token     Zugmarker (Ritterfigur)
 */
public record Stage(ClientGameLogic logic, BoardView board, HandView hand, Hud hud, DicePanel dicePanel, DiceView dice,
                    PromptView prompts, Fx fx, CameraRig rig, SoundManager sounds, Animator animator,
                    ScreenBridge bridge, TurnToken token) {

    /**
     * @return Spielzustand
     */
    public ClientGameState model() {
        return logic.getModel();
    }

    /**
     * @param player Spielername
     * @return Platznummer oder −1
     */
    public int seat(String player) {
        return player == null ? -1 : board.seatOf(player);
    }

    /**
     * @param player Spielername
     * @return {@code true} für den eigenen Spieler
     */
    public boolean isMe(String player) {
        return player != null && player.equals(model().getMyName());
    }

    /**
     * @param player Spielername
     * @return Spielerfarbe
     */
    public ColorRGBA color(String player) {
        int seat = seat(player);
        return seat < 0 ? Theme.CREAM.clone() : Theme.playerColor(seat);
    }

    /**
     * @param player Spielername
     * @return Mitte des Platzes
     */
    public Vector3f seatCenter(String player) {
        int seat = seat(player);
        return seat < 0 ? board.layout().center() : board.layout().seatCenter(seat);
    }
}
