package pp.winf2slay.controller.message.client;

import com.jme3.network.serializing.Serializable;
import pp.winf2slay.model.card.Challenge;

/**
 * Antwort auf eine Herausforderungsanfrage.
 *
 * <p>Ist {@link #getChallenge()} {@code null}, verzichtet der Spieler.</p>
 */
@Serializable
public class CMChallengeResponse extends ClientMessage {

    private Challenge challenge;

    private CMChallengeResponse() {
        // für @Serializable
    }

    /**
     * @param challenge gespielte Herausforderung oder {@code null} für „Nein“
     */
    public CMChallengeResponse(Challenge challenge) {
        this.challenge = challenge;
    }

    /**
     * @return gespielte Herausforderung oder {@code null} für „Nein“
     */
    public Challenge getChallenge() {
        return challenge;
    }

    /**
     * @return Antwort „keine Herausforderung“
     */
    public static CMChallengeResponse decline() {
        return new CMChallengeResponse(null);
    }

    /**
     * @return {@code true}, wenn eine Herausforderung gespielt wird
     */
    public boolean isChallenging() {
        return challenge != null;
    }

    @Override
    public void accept(ClientMessageInterpreter interpreter, int from) {
        interpreter.received(this, from);
    }

    @Override
    public String toString() {
        return "CMChallengeResponse[" + "challenge=" + challenge + "]";
    }
}
