package com.DBArena.services.identity.service;

import com.DBArena.common.core.error.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException(String email) {
        super("auth.email_already_registered", "An account already exists for this email address");
    }
}
