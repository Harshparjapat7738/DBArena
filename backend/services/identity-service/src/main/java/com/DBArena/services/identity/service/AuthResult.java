package com.DBArena.services.identity.service;

import com.DBArena.services.identity.domain.UserAccount;

import java.time.Instant;

public record AuthResult(UserAccount user, String accessToken, String refreshToken, Instant refreshTokenExpiresAt) {
}
