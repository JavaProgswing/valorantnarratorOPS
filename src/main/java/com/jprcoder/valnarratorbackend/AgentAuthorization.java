package com.jprcoder.valnarratorbackend;

/**
 * Outcome of the secure backend authorization for a premium agent (NeuTTS) voice request.
 * The server (see {@code /agentvoice/authorize}) is the single source of truth: the client
 * proceeds to synthesize only when {@link #status()} is {@link AgentAuthStatus#ACCEPTED}.
 *
 * @param status         accepted / rejected-quota / rejected-disabled / error.
 * @param remainingChars free-trial characters left for a non-premium user; -1 means unlimited
 *                       (premium) and is meaningless for rejections.
 * @param premium        whether the backend considers this account premium.
 * @param message        optional human-readable reason (e.g. why a voice is disabled); may be null.
 */
public record AgentAuthorization(AgentAuthStatus status, int remainingChars, boolean premium, String message) {

    public enum AgentAuthStatus {
        /** Proceed with synthesis. */
        ACCEPTED,
        /** Free agent-voice trial exhausted for this user; prompt for premium. */
        REJECTED_QUOTA,
        /** This voice (or all agent voices) has been disabled for quality/other reasons. */
        REJECTED_DISABLED,
        /** Could not verify with the backend (network/parse failure). Fail closed - do not proceed. */
        ERROR
    }

    public boolean isAccepted() {
        return status == AgentAuthStatus.ACCEPTED;
    }

    static AgentAuthorization error() {
        return new AgentAuthorization(AgentAuthStatus.ERROR, 0, false, null);
    }
}
