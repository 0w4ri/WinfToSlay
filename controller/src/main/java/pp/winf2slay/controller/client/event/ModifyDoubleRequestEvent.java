package pp.winf2slay.controller.client.event;

import pp.winf2slay.model.die.DiceResult;

/**
 * Frage: Soll einer der Würfe einer Herausforderung modifiziert werden?
 *
 * @param activeResult Wurf des herausgeforderten Spielers
 * @param challengerResult Wurf des Herausforderers
 */
public record ModifyDoubleRequestEvent(DiceResult activeResult, DiceResult challengerResult) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onModifyDoubleRequest(this);
    }
}
