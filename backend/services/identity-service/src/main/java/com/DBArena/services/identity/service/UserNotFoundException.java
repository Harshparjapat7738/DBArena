package com.DBArena.services.identity.service;

import com.DBArena.common.core.error.NotFoundException;
import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.identity.domain.UserAccount;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(TypedId<UserAccount> id) {
        super("User", id.value());
    }
}
