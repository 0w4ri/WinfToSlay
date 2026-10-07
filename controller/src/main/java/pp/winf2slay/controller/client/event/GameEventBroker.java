package pp.winf2slay.controller.client.event;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Verteilt {@link GameEvent}s an alle angemeldeten Listener.
 *
 * <p>Listener dürfen sich auch während der Verteilung an- oder abmelden.</p>
 */
public class GameEventBroker {

    private static final Logger LOGGER = System.getLogger(GameEventBroker.class.getName());

    private final List<GameEventListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * @param listener anzumeldender Listener
     */
    public void addListener(GameEventListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    /**
     * @param listener abzumeldender Listener
     */
    public void removeListener(GameEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Benachrichtigt alle Listener.
     *
     * @param event Ereignis
     */
    public void notifyListeners(GameEvent event) {
        for (GameEventListener listener : listeners) {
            try {
                event.notifyListener(listener);
            }
            catch (RuntimeException e) {
                // Ein fehlerhafter Listener darf die übrigen (z. B. die Spiellogik) nicht aufhalten.
                LOGGER.log(Level.ERROR, "Fehler beim Verarbeiten von " + event.getClass().getSimpleName(), e);
            }
        }
    }
}
