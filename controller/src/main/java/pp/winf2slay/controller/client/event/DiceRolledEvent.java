package pp.winf2slay.controller.client.event;

import pp.winf2slay.controller.message.RollPurpose;
import pp.winf2slay.model.die.DiceResult;

/**
 * Ein Spieler hat gewürfelt.
 *
 * @param result Ergebnis
 * @param purpose Anlass
 * @param threshold Schwellenwert
 * @param higherWins Richtung des Schwellenwerts
 */
public record DiceRolledEvent(DiceResult result, RollPurpose purpose, int threshold, boolean higherWins) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onDiceRolled(this);
    }
}
