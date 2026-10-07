package pp.winf2slay.view.anim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TweensTest {

    @Test
    void lerpEndsExactlyAtOne() {
        float[] last = {-1};
        Tween t = Tweens.lerp(1f, v -> last[0] = v);
        assertTrue(t.update(0.4f));
        assertEquals(0.4f, last[0], 1e-5);
        assertFalse(t.update(0.7f));
        assertEquals(1f, last[0], 1e-5);
    }

    @Test
    void sequenceRunsInOrderAndFinishRunsRemainingSteps() {
        List<String> log = new ArrayList<>();
        Tween t = Tweens.seq(Tweens.call(() -> log.add("a")), Tweens.delay(1f), Tweens.call(() -> log.add("b")),
                             Tweens.delay(1f), Tweens.call(() -> log.add("c")));
        t.update(0.5f);
        assertEquals(List.of("a"), log);
        t.update(0.6f);
        assertEquals(List.of("a", "b"), log);
        t.finish();
        assertEquals(List.of("a", "b", "c"), log);
    }

    @Test
    void parallelEndsWithLongestPart() {
        Tween t = Tweens.par(Tweens.delay(0.2f), Tweens.delay(1f));
        assertTrue(t.update(0.5f));
        assertFalse(t.update(0.6f));
    }

    @Test
    void deferCreatesTweenWhenItsTurnComes() {
        List<String> log = new ArrayList<>();
        Tween t = Tweens.seq(Tweens.call(() -> log.add("first")),
                             Tweens.defer(() -> {
                                 log.add("created");
                                 return Tweens.call(() -> log.add("run"));
                             }));
        assertTrue(log.isEmpty());
        t.update(0.01f);
        assertEquals(List.of("first", "created", "run"), log);
    }

    @Test
    void easingsMapEndpoints() {
        for (Easing e : List.of(Easing.LINEAR, Easing.OUT_QUAD, Easing.IN_OUT_CUBIC, Easing.OUT_BACK,
                                Easing.OUT_ELASTIC, Easing.OUT_BOUNCE, Easing.IN_CUBIC)) {
            assertEquals(0f, e.apply(0f), 1e-4);
            assertEquals(1f, e.apply(1f), 1e-4);
        }
    }
}
