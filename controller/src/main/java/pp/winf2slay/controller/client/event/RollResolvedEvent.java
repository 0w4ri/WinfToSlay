package pp.winf2slay.controller.client.event;

import pp.winf2slay.controller.message.server.BCRollResolved;

/**
 * Ein Heldeneffekt- oder Angriffswurf ist entschieden.
 *
 * @param resolution Ergebnis
 */
public record RollResolvedEvent(BCRollResolved resolution) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onRollResolved(this);
    }
}
