package pp.winf2slay.controller.client.event;

/**
 * Ein Hinweis soll angezeigt werden (z. B. „Zu wenige Aktionspunkte“).
 *
 * @param text Hinweistext
 * @param warning {@code true} für Warnungen/Fehler
 */
public record NoticeEvent(String text, boolean warning) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onNotice(this);
    }
}
