package com.DBArena.services.identity.service;

import com.DBArena.common.core.id.IdGenerator;
import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.identity.config.IdentityProperties;
import com.DBArena.services.identity.domain.RefreshTokenRecord;
import com.DBArena.services.identity.domain.UserAccount;
import com.DBArena.services.identity.repository.RefreshTokenRepository;
import com.DBArena.services.identity.repository.UserRepository;
import com.DBArena.services.identity.security.JwtIssuer;
import com.DBArena.services.identity.security.PasswordHasher;
import com.DBArena.services.identity.security.RefreshTokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordHasher passwordHasher;
    private JwtIssuer jwtIssuer;
    private RefreshTokenGenerator refreshTokenGenerator;
    private IdGenerator idGenerator;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        jwtIssuer = mock(JwtIssuer.class);
        refreshTokenGenerator = mock(RefreshTokenGenerator.class);

        AtomicInteger idCounter = new AtomicInteger();
        idGenerator = mock(IdGenerator.class);
        when(idGenerator.next()).thenAnswer(inv -> "id-" + idCounter.incrementAndGet());
        when(idGenerator.nextTyped()).thenAnswer(inv -> TypedId.of("id-" + idCounter.incrementAndGet()));

        when(jwtIssuer.issueAccessToken(any(), any())).thenReturn("fake-access-token");
        when(refreshTokenGenerator.generate()).thenReturn("fake-refresh-plaintext");
        when(refreshTokenGenerator.hash(anyString())).thenAnswer(inv -> "hash(" + inv.getArgument(0) + ")");

        IdentityProperties properties = new IdentityProperties();
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setRefreshTokenTtl(Duration.ofDays(30));

        authService = new AuthService(userRepository, refreshTokenRepository, passwordHasher, jwtIssuer,
                refreshTokenGenerator, idGenerator, properties, FIXED_CLOCK);
    }

    @Test
    void registerRejectsADuplicateEmail() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("taken@example.com", "password1234", "Ada"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(userRepository, never()).insert(any());
    }

    @Test
    void registerInsertsUserWithDefaultRoleAndIssuesASession() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordHasher.hash("password1234")).thenReturn("hashed-password");

        AuthResult result = authService.register("new@example.com", "password1234", "Ada");

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).insert(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("new@example.com");
        assertThat(captor.getValue().roles()).containsExactly(UserAccount.DEFAULT_ROLE);
        assertThat(captor.getValue().passwordHash()).isEqualTo("hashed-password");

        assertThat(result.accessToken()).isEqualTo("fake-access-token");
        assertThat(result.refreshToken()).isEqualTo("fake-refresh-plaintext");
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody@example.com", "whatever12345"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        UserAccount user = sampleUser();
        when(userRepository.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong", user.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(user.email(), "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginSucceedsAndIssuesASession() {
        UserAccount user = sampleUser();
        when(userRepository.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(passwordHasher.matches("correct-password", user.passwordHash())).thenReturn(true);

        AuthResult result = authService.login(user.email(), "correct-password");

        assertThat(result.user()).isEqualTo(user);
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void refreshRotatesTheTokenAndChainsReplacedById() {
        UserAccount user = sampleUser();
        RefreshTokenRecord existing = new RefreshTokenRecord(
                "old-token-id", user.id(), "hash(presented)",
                Instant.parse("2025-12-01T00:00:00Z"), Instant.parse("2026-06-01T00:00:00Z"),
                Optional.empty(), Optional.empty());

        when(refreshTokenGenerator.hash("presented")).thenReturn("hash(presented)");
        when(refreshTokenRepository.findByTokenHash("hash(presented)")).thenReturn(Optional.of(existing));
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        AuthResult result = authService.refresh("presented");

        assertThat(result.user()).isEqualTo(user);
        ArgumentCaptor<Optional<String>> replacedByCaptor = ArgumentCaptor.forClass(Optional.class);
        verify(refreshTokenRepository).revoke(eq("old-token-id"), eq(FIXED_CLOCK.instant()), replacedByCaptor.capture());
        assertThat(replacedByCaptor.getValue()).isPresent();
        verify(refreshTokenRepository, never()).revokeAllForUser(any(), any());
    }

    @Test
    void refreshOfAnAlreadyRevokedTokenRevokesEverySessionForTheUser() {
        UserAccount user = sampleUser();
        RefreshTokenRecord alreadyRevoked = new RefreshTokenRecord(
                "old-token-id", user.id(), "hash(presented)",
                Instant.parse("2025-12-01T00:00:00Z"), Instant.parse("2026-06-01T00:00:00Z"),
                Optional.of(Instant.parse("2025-12-15T00:00:00Z")), Optional.of("some-other-id"));

        when(refreshTokenGenerator.hash("presented")).thenReturn("hash(presented)");
        when(refreshTokenRepository.findByTokenHash("hash(presented)")).thenReturn(Optional.of(alreadyRevoked));

        assertThatThrownBy(() -> authService.refresh("presented"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository).revokeAllForUser(user.id(), FIXED_CLOCK.instant());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void refreshOfAnExpiredTokenFails() {
        UserAccount user = sampleUser();
        RefreshTokenRecord expired = new RefreshTokenRecord(
                "old-token-id", user.id(), "hash(presented)",
                Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-02-01T00:00:00Z"),
                Optional.empty(), Optional.empty());

        when(refreshTokenGenerator.hash("presented")).thenReturn("hash(presented)");
        when(refreshTokenRepository.findByTokenHash("hash(presented)")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh("presented"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshOfAnUnknownTokenFails() {
        when(refreshTokenGenerator.hash(anyString())).thenReturn("hash(x)");
        when(refreshTokenRepository.findByTokenHash("hash(x)")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("nope")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logoutRevokesAnUnrevokedToken() {
        UserAccount user = sampleUser();
        RefreshTokenRecord live = new RefreshTokenRecord(
                "token-id", user.id(), "hash(presented)",
                Instant.parse("2025-12-01T00:00:00Z"), Instant.parse("2026-06-01T00:00:00Z"),
                Optional.empty(), Optional.empty());

        when(refreshTokenGenerator.hash("presented")).thenReturn("hash(presented)");
        when(refreshTokenRepository.findByTokenHash("hash(presented)")).thenReturn(Optional.of(live));

        authService.logout("presented");

        verify(refreshTokenRepository).revoke("token-id", FIXED_CLOCK.instant(), Optional.empty());
    }

    @Test
    void logoutOfAnAlreadyRevokedTokenIsANoOp() {
        UserAccount user = sampleUser();
        RefreshTokenRecord revoked = new RefreshTokenRecord(
                "token-id", user.id(), "hash(presented)",
                Instant.parse("2025-12-01T00:00:00Z"), Instant.parse("2026-06-01T00:00:00Z"),
                Optional.of(Instant.parse("2025-12-10T00:00:00Z")), Optional.empty());

        when(refreshTokenGenerator.hash("presented")).thenReturn("hash(presented)");
        when(refreshTokenRepository.findByTokenHash("hash(presented)")).thenReturn(Optional.of(revoked));

        authService.logout("presented");

        verify(refreshTokenRepository, never()).revoke(anyString(), any(), any());
    }

    private static UserAccount sampleUser() {
        return new UserAccount(TypedId.of("user-1"), "ada@example.com", "hashed",
                "Ada", Set.of("learner"), Instant.parse("2025-01-01T00:00:00Z"));
    }
}
