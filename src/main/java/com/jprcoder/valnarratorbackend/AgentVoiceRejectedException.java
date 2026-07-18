package com.jprcoder.valnarratorbackend;

/**
 * Thrown when the local agent-voice server refuses to synthesize because the backend did not
 * authorize the request (free trial exhausted, voice disabled, or the identity/verification
 * failed). Carries the mapped reason so the UI can react appropriately (upgrade prompt vs. alert).
 */
public class AgentVoiceRejectedException extends Exception {
    private final AgentAuthorization.AgentAuthStatus status;

    public AgentVoiceRejectedException(AgentAuthorization.AgentAuthStatus status, String message) {
        super(message);
        this.status = status;
    }

    public AgentAuthorization.AgentAuthStatus status() {
        return status;
    }

    /** Maps the backend/server reason string to a status. Unknown reasons fail closed as ERROR. */
    public static AgentAuthorization.AgentAuthStatus fromReason(String reason) {
        if (reason == null) return AgentAuthorization.AgentAuthStatus.ERROR;
        return switch (reason.toLowerCase()) {
            case "quota" -> AgentAuthorization.AgentAuthStatus.REJECTED_QUOTA;
            case "disabled" -> AgentAuthorization.AgentAuthStatus.REJECTED_DISABLED;
            default -> AgentAuthorization.AgentAuthStatus.ERROR;
        };
    }
}
