package pp.winf2slay.controller.client.event;

import pp.winf2slay.controller.message.server.BCChallengeModified;

/**
 * Ein Wurf einer Herausforderung wurde modifiziert.
 *
 * @param modification Details
 */
public record ChallengeModifiedEvent(BCChallengeModified modification) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onChallengeModified(this);
    }
}
