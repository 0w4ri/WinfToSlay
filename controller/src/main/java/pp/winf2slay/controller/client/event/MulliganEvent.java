package pp.winf2slay.controller.client.event;

import pp.winf2slay.controller.message.server.BCMulligan;

/**
 * Ein Spieler hat einen Mulligan durchgeführt.
 *
 * @param mulligan Details
 */
public record MulliganEvent(BCMulligan mulligan) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onMulligan(this);
    }
}
