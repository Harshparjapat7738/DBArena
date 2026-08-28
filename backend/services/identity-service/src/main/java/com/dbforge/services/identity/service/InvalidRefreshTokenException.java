package com.dbforge.services.identity.service;

import com.dbforge.common.core.error.DomainException;

public class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException(String reason) {
        super("auth.invalid_refresh_token", 401, "Refresh token is invalid: " + reason);
    }
}
