package pp.winf2slay.controller.client.event;

import pp.winf2slay.controller.message.server.BCChallengeRolled;

/**
 * Beide Seiten einer Herausforderung haben gewürfelt.
 *
 * @param roll Würfe
 */
public record ChallengeRolledEvent(BCChallengeRolled roll) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onChallengeRolled(this);
    }
}
