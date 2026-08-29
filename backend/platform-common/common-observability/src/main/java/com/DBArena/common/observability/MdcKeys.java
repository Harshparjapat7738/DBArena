package com.DBArena.common.observability;

/** MDC key names every service's log lines and every structured-logging config agree on. */
public final class MdcKeys {

    private MdcKeys() {
    }

    /** Propagated end-to-end via the {@code X-Correlation-Id} header; generated if absent. */
    public static final String CORRELATION_ID = "correlationId";

    /** Set once a request is authenticated - see common-security's JwtAuthenticationFilter integration point. */
    public static final String USER_ID = "userId";
}
