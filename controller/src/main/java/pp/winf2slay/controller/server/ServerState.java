package pp.winf2slay.controller.server;

import pp.winf2slay.controller.message.client.CMActivateHeroEffect;
import pp.winf2slay.controller.message.client.CMAddBot;
import pp.winf2slay.controller.message.client.CMAnimationsDone;
import pp.winf2slay.controller.message.client.CMAttackMonster;
import pp.winf2slay.controller.message.client.CMChallengeResponse;
import pp.winf2slay.controller.message.client.CMDrawCard;
import pp.winf2slay.controller.message.client.CMEndTurn;
import pp.winf2slay.controller.message.client.CMHeroSelectionResponse;
import pp.winf2slay.controller.message.client.CMJoinLobby;
import pp.winf2slay.controller.message.client.CMLeaveLobby;
import pp.winf2slay.controller.message.client.CMModifyDoubleResponse;
import pp.winf2slay.controller.message.client.CMModifySingleResponse;
import pp.winf2slay.controller.message.client.CMMulligan;
import pp.winf2slay.controller.message.client.CMPlayCard;
import pp.winf2slay.controller.message.client.CMPlayerSelectionResponse;
import pp.winf2slay.controller.message.client.CMRemoveBot;
import pp.winf2slay.controller.message.client.CMStartGame;
import pp.winf2slay.controller.message.client.ClientMessage;
import pp.winf2slay.controller.message.client.ClientMessageInterpreter;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Basisklasse der Serverzustände (Lobby, laufendes Spiel, Spielende).
 *
 * <p>Jede Nachricht, die ein Zustand nicht ausdrücklich verarbeitet, wird
 * protokolliert und verworfen.</p>
 */
public abstract class ServerState implements ClientMessageInterpreter {

    protected static final Logger LOGGER = System.getLogger(ServerState.class.getName());

    protected final ServerGameLogic logic;

    /**
     * @param logic Serverlogik
     */
    protected ServerState(ServerGameLogic logic) {
        this.logic = logic;
    }

    /**
     * Wird beim Betreten des Zustands aufgerufen.
     */
    public void entry() {
        // Standard: nichts zu tun
    }

    /**
     * @return {@code true}, solange eine Partie läuft
     */
    public boolean isPlaying() {
        return false;
    }

    /**
     * Ein Client hat sich verbunden.
     *
     * @param id Verbindungs-ID
     * @return {@code false}, wenn der Client abgewiesen wird
     */
    public boolean playerConnected(int id) {
        LOGGER.log(Level.INFO, "Verbindung {0} abgewiesen im Zustand {1}", id, this);
        return false;
    }

    /**
     * Ein Client hat die Verbindung getrennt.
     *
     * @param id Verbindungs-ID
     */
    public void playerDisconnected(int id) {
        LOGGER.log(Level.INFO, "Verbindung {0} getrennt im Zustand {1}", id, this);
    }

    /**
     * Protokolliert eine im aktuellen Zustand nicht erlaubte Nachricht.
     *
     * @param msg  Nachricht
     * @param from Absender
     */
    protected void unexpected(ClientMessage msg, int from) {
        LOGGER.log(Level.WARNING, "{0} von {1} ist im Zustand {2} nicht erlaubt", msg, from, this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    // ------------------------------------------------------------------
    // Standardverhalten: Nachricht ist nicht erlaubt
    // ------------------------------------------------------------------

    @Override
    public void received(CMJoinLobby msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMLeaveLobby msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMAddBot msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMRemoveBot msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMStartGame msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMDrawCard msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMPlayCard msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMActivateHeroEffect msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMAttackMonster msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMMulligan msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMEndTurn msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMChallengeResponse msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMModifySingleResponse msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMModifyDoubleResponse msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMHeroSelectionResponse msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMPlayerSelectionResponse msg, int from) {
        unexpected(msg, from);
    }

    @Override
    public void received(CMAnimationsDone msg, int from) {
        unexpected(msg, from);
    }
}
