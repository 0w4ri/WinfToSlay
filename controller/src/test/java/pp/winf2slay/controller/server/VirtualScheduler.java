package pp.winf2slay.controller.server;

import java.util.PriorityQueue;
import java.util.function.BooleanSupplier;

/**
 * Scheduler mit virtueller Zeit für Tests: Aufgaben werden ohne echtes Warten
 * in zeitlicher Reihenfolge ausgeführt.
 */
public class VirtualScheduler implements ServerScheduler {

    private record Task(long time, long seq, Runnable runnable) {}

    private final PriorityQueue<Task> queue = new PriorityQueue<>((a, b) -> a.time != b.time
                                                                            ? Long.compare(a.time, b.time)
                                                                            : Long.compare(a.seq, b.seq));
    private long now;
    private long seq;
    private int executed;

    @Override
    public void schedule(Runnable task, long delayMillis) {
        queue.add(new Task(now + Math.max(0, delayMillis), seq++, task));
    }

    /**
     * Führt Aufgaben aus, bis die Bedingung erfüllt ist, keine Aufgaben mehr
     * anstehen oder das Limit erreicht ist.
     *
     * @param done     Abbruchbedingung
     * @param maxTasks Höchstzahl auszuführender Aufgaben
     * @return {@code true}, wenn die Bedingung erfüllt wurde
     */
    public boolean runUntil(BooleanSupplier done, int maxTasks) {
        int count = 0;
        while (!done.getAsBoolean() && !queue.isEmpty() && count < maxTasks) {
            Task task = queue.poll();
            now = task.time();
            task.runnable().run();
            count++;
            executed++;
        }
        return done.getAsBoolean();
    }

    /**
     * Führt alle anstehenden Aufgaben aus, die bis jetzt fällig sind
     * (ohne die virtuelle Zeit vorzustellen).
     */
    public void runPending() {
        while (!queue.isEmpty() && queue.peek().time() <= now) {
            queue.poll().runnable().run();
            executed++;
        }
    }

    /**
     * @return virtuelle Zeit in Millisekunden
     */
    public long getNow() {
        return now;
    }

    /**
     * @return Anzahl insgesamt ausgeführter Aufgaben
     */
    public int getExecuted() {
        return executed;
    }
}
