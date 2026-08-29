package com.DBArena.services.identity.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.identity.domain.UserAccount;

import java.util.Optional;

public interface UserRepository {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(TypedId<UserAccount> id);

    boolean existsByEmail(String email);

    /** Inserts the user row and its role rows. Caller (AuthService) owns the transaction boundary. */
    void insert(UserAccount user);
}
