package pp.winf2slay.controller.client.event;

/**
 * Ob dieser Client gerade Aktionen ausführen darf, hat sich geändert.
 *
 * @param canAct {@code true}, wenn Aktionen möglich sind
 * @param actionPoints verbleibende Aktionspunkte
 */
public record ActionStateChangedEvent(boolean canAct, int actionPoints) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onActionStateChanged(this);
    }
}
