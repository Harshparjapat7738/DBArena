package com.dbforge.services.ai.provider;

/**
 * One provider call failed - not configured (no API key), timed out,
 * returned a non-2xx, or returned a shape this client couldn't parse.
 * Never wraps the raw learner content back into the message (nothing here
 * should end up echoing a prompt injection attempt from a learner's own
 * pasted query/error text into a log line at higher-than-debug level).
 */
public class AiProviderException extends RuntimeException {

    public AiProviderException(String providerName, String reason) {
        super(providerName + ": " + reason);
    }

    public AiProviderException(String providerName, String reason, Throwable cause) {
        super(providerName + ": " + reason, cause);
    }
}
