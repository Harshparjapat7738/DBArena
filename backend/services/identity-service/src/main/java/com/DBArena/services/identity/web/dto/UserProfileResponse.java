package com.DBArena.services.identity.web.dto;

import com.DBArena.services.identity.domain.UserAccount;

import java.util.Set;

/** Never includes passwordHash - the one and only place a UserAccount is turned into a response. */
public record UserProfileResponse(String id, String email, String displayName, Set<String> roles) {

    public static UserProfileResponse from(UserAccount user) {
        return new UserProfileResponse(user.id().value(), user.email(), user.displayName(), user.roles());
    }
}
