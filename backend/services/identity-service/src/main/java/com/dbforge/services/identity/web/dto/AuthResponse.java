package com.dbforge.services.identity.web.dto;

/**
 * The refresh token is deliberately NOT a field here - it only ever
 * leaves this service in the HttpOnly Set-Cookie header (root CLAUDE.md
 * hard rule #6). {@code accessToken} in the body is fine: the frontend
 * keeps it in memory only, never in localStorage - see frontend/CLAUDE.md.
 */
public record AuthResponse(String accessToken, UserProfileResponse user) {
}
