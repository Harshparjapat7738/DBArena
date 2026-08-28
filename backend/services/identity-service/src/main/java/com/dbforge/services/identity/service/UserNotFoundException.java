package com.dbforge.services.identity.service;

import com.dbforge.common.core.error.NotFoundException;
import com.dbforge.common.core.id.TypedId;
import com.dbforge.services.identity.domain.UserAccount;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(TypedId<UserAccount> id) {
        super("User", id.value());
    }
}
