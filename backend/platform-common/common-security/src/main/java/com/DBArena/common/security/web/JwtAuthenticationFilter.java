package com.DBArena.common.security.web;

import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.common.security.context.CurrentUserContext;
import com.DBArena.common.security.jwt.JwtVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Resolves the {@code Authorization: Bearer <token>} header into a
 * {@link AuthenticatedUser} bound to {@link CurrentUserContext} for the
 * duration of the request. Deliberately does not reject unauthenticated
 * requests itself - a missing or invalid token just means
 * {@link CurrentUserContext#get()} is empty; endpoints that require auth
 * enforce that via {@code @RequiresRole} or their own check, so public
 * endpoints keep working through the same filter chain.
 *
 * <p>Per root CLAUDE.md hard rule #6, this filter only ever reads the
 * token from the header - it must never be adapted to read from a cookie
 * named for the access token, and the access token this resolves must
 * never be written back to a cookie or to {@code localStorage} by any
 * caller.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtVerifier jwtVerifier;

    public JwtAuthenticationFilter(JwtVerifier jwtVerifier) {
        this.jwtVerifier = jwtVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            extractBearerToken(request)
                    .flatMap(jwtVerifier::verify)
                    .ifPresent(CurrentUserContext::set);
            chain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
        }
    }

    private static Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
