package com.DBArena.services.identity.web;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.common.security.web.CurrentUser;
import com.DBArena.services.identity.domain.UserAccount;
import com.DBArena.services.identity.service.AuthResult;
import com.DBArena.services.identity.service.AuthService;
import com.DBArena.services.identity.service.InvalidRefreshTokenException;
import com.DBArena.services.identity.web.dto.AuthResponse;
import com.DBArena.services.identity.web.dto.LoginRequest;
import com.DBArena.services.identity.web.dto.RegisterRequest;
import com.DBArena.services.identity.web.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "auth", description = "Registration, login, and refresh-token session management")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;

    public AuthController(AuthService authService, RefreshCookieFactory refreshCookieFactory) {
        this.authService = authService;
        this.refreshCookieFactory = refreshCookieFactory;
    }

    @PostMapping("/register")
    @Operation(summary = "Create an account and start a session")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result = authService.register(request.email(), request.password(), request.displayName());
        return withRefreshCookie(result, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Start a session for an existing account")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request.email(), request.password());
        return withRefreshCookie(result, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token in the cookie for a new access token")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("missing");
        }
        AuthResult result = authService.refresh(refreshToken);
        return withRefreshCookie(result, HttpStatus.OK);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current session's refresh token and clear the cookie")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.clear().toString())
                .build();
    }

    @GetMapping("/me")
    @Operation(summary = "The caller's own profile, resolved from the access token")
    public UserProfileResponse me(@CurrentUser AuthenticatedUser currentUser) {
        UserAccount user = authService.requireUser(TypedId.of(currentUser.userId().value()));
        return UserProfileResponse.from(user);
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthResult result, HttpStatus status) {
        AuthResponse body = new AuthResponse(result.accessToken(), UserProfileResponse.from(result.user()));
        String cookie = refreshCookieFactory.issue(result.refreshToken(), result.refreshTokenExpiresAt()).toString();
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie)
                .body(body);
    }
}
