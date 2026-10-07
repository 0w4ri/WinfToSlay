package pp.winf2slay.controller.server;

/**
 * Führt Aufgaben (zeitversetzt) im Server-Thread aus.
 *
 * <p>Die gesamte Serverlogik ist bewusst nicht threadsicher: Alles läuft in
 * genau einem Thread. Bots und Zeitüberschreitungen planen ihre Aktionen
 * deshalb über diese Schnittstelle ein, statt die Logik direkt aufzurufen.</p>
 */
public interface ServerScheduler {

    /**
     * Plant eine Aufgabe ein.
     *
     * @param task        auszuführende Aufgabe
     * @param delayMillis Verzögerung in Millisekunden (0 = so bald wie möglich)
     */
    void schedule(Runnable task, long delayMillis);
}
