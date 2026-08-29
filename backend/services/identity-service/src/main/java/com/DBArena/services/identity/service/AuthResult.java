package com.dbforge.services.identity.service;

import com.dbforge.services.identity.domain.UserAccount;

import java.time.Instant;

public record AuthResult(UserAccount user, String accessToken, String refreshToken, Instant refreshTokenExpiresAt) {
}
