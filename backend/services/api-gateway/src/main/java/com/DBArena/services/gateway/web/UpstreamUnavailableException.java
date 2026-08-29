package com.dbforge.services.gateway.web;

import com.dbforge.common.core.error.DomainException;

public class UpstreamUnavailableException extends DomainException {

    public UpstreamUnavailableException(String prefix, Throwable cause) {
        super("gateway.upstream_unavailable", 502, "Upstream service for " + prefix + " is unavailable");
        initCause(cause);
    }
}
