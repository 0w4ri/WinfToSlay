package pp.winf2slay.controller.client.event;

/**
 * Ereignis der Client-Logik für die Oberfläche.
 *
 * <p>Ereignisse werden über den {@link GameEventBroker} an alle angemeldeten
 * {@link GameEventListener} verteilt (Double-Dispatch).</p>
 */
public interface GameEvent {

    /**
     * Ruft die passende Methode des Listeners auf.
     *
     * @param listener Empfänger
     */
    void notifyListener(GameEventListener listener);
}
