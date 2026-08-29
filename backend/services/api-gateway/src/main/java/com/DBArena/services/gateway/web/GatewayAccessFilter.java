package com.dbforge.services.gateway.web;

import com.dbforge.common.security.context.CurrentUserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Coarse edge check: a non-public path with no {@link CurrentUserContext}
 * bound (common-security's JwtAuthenticationFilter, which runs before
 * this one, populates it) is rejected here so an invalid request never
 * even reaches a backend service. This is a convenience, not the
 * security boundary - every backend service verifies the token itself
 * too, independently.
 */
public class GatewayAccessFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public GatewayAccessFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (PublicPaths.isPublic(request.getMethod(), path) || CurrentUserContext.get().isPresent()) {
            chain.doFilter(request, response);
            return;
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "A valid access token is required for " + path);
        problem.setProperty("code", "auth.unauthenticated");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
