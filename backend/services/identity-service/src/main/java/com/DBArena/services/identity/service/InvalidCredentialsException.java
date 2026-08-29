package com.DBArena.services.identity.service;

import com.DBArena.common.core.error.DomainException;

/** Deliberately the same error for "no such email" and "wrong password" - do not let this distinguish the two to a caller. */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("auth.invalid_credentials", 401, "Email or password is incorrect");
    }
}
