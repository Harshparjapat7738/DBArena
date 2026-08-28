package com.dbforge.services.gateway.web;

import com.dbforge.common.core.error.DomainException;

public class NoRouteFoundException extends DomainException {

    public NoRouteFoundException(String path) {
        super("gateway.no_route", 404, "No route configured for path: " + path);
    }
}
