package pp.winf2slay.controller.client;

/**
 * Entscheidet, wann ein Broadcast bestätigt wird.
 *
 * <p>Nach jedem Broadcast ruft die Client-Logik {@link #afterAnimations(Runnable)}
 * genau einmal auf. Die Oberfläche hängt die Bestätigung hinter ihre Animationen;
 * ohne Oberfläche (Tests) wird sofort bestätigt.</p>
 */
@FunctionalInterface
public interface AnimationGate {

    /**
     * Bestätigt sofort – für Tests und Clients ohne Animationen.
     */
    AnimationGate IMMEDIATE = Runnable::run;

    /**
     * Führt {@code done} aus, sobald alle bis jetzt angestoßenen Animationen beendet sind.
     *
     * @param done Bestätigung; muss genau einmal ausgeführt werden
     */
    void afterAnimations(Runnable done);
}
