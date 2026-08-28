package com.dbforge.services.identity.service;

import com.dbforge.common.core.error.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException(String email) {
        super("auth.email_already_registered", "An account already exists for this email address");
    }
}
