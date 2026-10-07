package pp.winf2slay.view.anim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimatorTest {

    @Test
    void gateConfirmsImmediatelyWhenIdle() {
        Animator animator = new Animator();
        boolean[] done = {false};
        animator.afterAnimations(() -> done[0] = true);
        assertTrue(done[0]);
    }

    @Test
    void gateWaitsForMainTrackAndRunsExactlyOnce() {
        Animator animator = new Animator();
        animator.enqueue(Tweens.delay(1f));
        animator.enqueue(Tweens.delay(1f));
        int[] count = {0};
        animator.afterAnimations(() -> count[0]++);
        animator.update(0.9f);
        assertEquals(0, count[0]);
        animator.update(0.9f);
        assertEquals(0, count[0]);
        animator.update(0.5f);
        assertEquals(1, count[0]);
        animator.update(1f);
        assertEquals(1, count[0]);
    }

    @Test
    void effectsDoNotBlockTheGate() {
        Animator animator = new Animator();
        animator.play(Tweens.delay(10f));
        assertFalse(animator.isBusy());
    }

    @Test
    void speedScalesMainTrack() {
        Animator animator = new Animator();
        animator.setSpeed(2f);
        animator.enqueue(Tweens.delay(1f));
        animator.update(0.6f);
        assertFalse(animator.isBusy());
    }

    @Test
    void mainTrackRunsSequentially() {
        Animator animator = new Animator();
        List<String> log = new ArrayList<>();
        animator.enqueue(Tweens.seq(Tweens.delay(0.5f), Tweens.call(() -> log.add("a"))));
        animator.enqueue(Tweens.call(() -> log.add("b")));
        animator.update(0.2f);
        assertTrue(log.isEmpty());
        animator.update(0.4f);
        assertEquals(List.of("a", "b"), log);
    }

    @Test
    void watchdogEndsHangingAnimations() {
        Animator animator = new Animator();
        animator.enqueue(Tweens.waitUntil(() -> false));
        boolean[] done = {false};
        animator.afterAnimations(() -> done[0] = true);
        for (int i = 0; i < 25; i++) animator.update(1f);
        assertTrue(done[0]);
        assertFalse(animator.isBusy());
    }
}
