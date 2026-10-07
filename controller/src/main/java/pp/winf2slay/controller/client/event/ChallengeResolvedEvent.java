package pp.winf2slay.controller.client.event;

import pp.winf2slay.controller.message.server.BCChallengeResolved;

/**
 * Eine Herausforderung ist entschieden.
 *
 * @param resolution Ergebnis
 */
public record ChallengeResolvedEvent(BCChallengeResolved resolution) implements GameEvent {

    @Override
    public void notifyListener(GameEventListener listener) {
        listener.onChallengeResolved(this);
    }
}
