package pp.winf2slay.controller.client.event;

import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.model.die.DiceResult;

/**
 * Frage: Soll ein Wurf modifiziert werden?
 *
 * @param result aktuelles Ergebnis
 * @param purpose Anlass
 * @param threshold Schwellenwert
 * @param higherWins Richtung des Schwellenwerts
 */
public record ModifySingleRequestEvent(DiceResult result, RollPurpose purpose, int threshold, boolean higherWins) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onModifySingleRequest(this);
    }
}
