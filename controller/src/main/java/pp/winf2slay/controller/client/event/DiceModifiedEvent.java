package pp.winf2slay.controller.client.event;

import pp.winf2slay.model.card.Modification;
import pp.winf2slay.model.die.DiceResult;

/**
 * Ein Wurf wurde durch eine Modifikation verändert.
 *
 * @param modifier Spieler, der die Modifikation gespielt hat
 * @param modification gespielte Karte
 * @param result neues Ergebnis
 */
public record DiceModifiedEvent(String modifier, Modification modification, DiceResult result) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onDiceModified(this);
    }
}
