package com.dbforge.services.gateway.web;

import java.util.Set;

/** RFC 7230 §6.1 hop-by-hop headers, plus Host/Content-Length which the HTTP client recomputes itself for the outgoing request. */
final class HopByHopHeaders {

    private HopByHopHeaders() {
    }

    static final Set<String> NAMES = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade", "host", "content-length");

    static boolean isHopByHop(String headerName) {
        return NAMES.contains(headerName.toLowerCase());
    }
}
