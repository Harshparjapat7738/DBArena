package com.DBArena.services.gateway.web;

import com.DBArena.common.core.error.DomainException;

public class NoRouteFoundException extends DomainException {

    public NoRouteFoundException(String path) {
        super("gateway.no_route", 404, "No route configured for path: " + path);
    }
}
