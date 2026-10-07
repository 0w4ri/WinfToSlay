package pp.winf2slay.controller.message.server;

import com.jme3.network.serializing.Serializable;

/**
 * Basisklasse aller Broadcast-Nachrichten.
 *
 * <p>Ein Broadcast beschreibt ein Spielereignis, das alle Clients animieren.
 * Jeder Empfänger erhält eine eigene Kopie mit seiner persönlichen Sicht auf den
 * Spielzustand ({@link ClientSync}) und bestätigt die Nachricht nach dem
 * Abspielen der Animationen mit {@code CMAnimationsDone(syncId)}. Erst wenn
 * alle Spieler bestätigt haben, setzt der Server das Spiel fort.</p>
 */
@Serializable
public abstract class BroadcastMessage extends ServerMessage implements Cloneable {

    private int syncId;
    private ClientSync sync;

    /**
     * @return Kennung, mit der der Empfang bestätigt wird
     */
    public int getSyncId() {
        return syncId;
    }

    /**
     * @return Spielzustand aus Sicht des Empfängers
     */
    public ClientSync getSync() {
        return sync;
    }

    /**
     * Erzeugt die Kopie für einen bestimmten Empfänger.
     *
     * <p>Unterklassen können {@link #hideFrom(String)} überschreiben, um geheime
     * Informationen (z. B. gezogene Karten) vor anderen Spielern zu verbergen.</p>
     *
     * @param syncId   Kennung des Broadcasts
     * @param viewer   Name des Empfängers
     * @param viewSync Spielzustand aus Sicht des Empfängers
     * @return persönliche Kopie
     */
    public BroadcastMessage personalizedFor(int syncId, String viewer, ClientSync viewSync) {
        try {
            BroadcastMessage copy = (BroadcastMessage) clone();
            copy.syncId = syncId;
            copy.sync = viewSync;
            copy.hideFrom(viewer);
            return copy;
        }
        catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Entfernt Informationen, die der Empfänger nicht sehen darf. Wird nur auf
     * der persönlichen Kopie aufgerufen.
     *
     * @param viewer Name des Empfängers
     */
    protected void hideFrom(String viewer) {
        // Standard: keine geheimen Informationen
    }
}
