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
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Orchestrates registration, login, and refresh-token rotation. No
 * {@code @Transactional} here (unlike the Postgres/JDBC version this
 * replaced - see MongoConfig's Javadoc for the store swap): every method
 * below is either a single document write (already atomic on its own -
 * {@code register}'s user document embeds its roles precisely so it
 * stays single-document, see UserDocumentMapper) or a sequence of
 * independently-atomic single-document writes where partial completion
 * is already a handled, meaningful state (e.g. {@code refresh} revoking
 * the old token after saving the new one - if the process died in
 * between, the old token is still usable next time, which is safe, not
 * corrupt). This bean is a Spring singleton shared across concurrent
 * requests, so it must hold no per-request mutable state - every method
 * threads its data through parameters and return values only.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final JwtIssuer jwtIssuer;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final IdGenerator idGenerator;
    private final IdentityProperties properties;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasher passwordHasher,
            JwtIssuer jwtIssuer,
            RefreshTokenGenerator refreshTokenGenerator,
            IdGenerator idGenerator,
            IdentityProperties properties,
            Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.jwtIssuer = jwtIssuer;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.idGenerator = idGenerator;
        this.properties = properties;
        this.clock = clock;
    }

    public AuthResult register(String email, String rawPassword, String displayName) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        UserAccount user = new UserAccount(
                idGenerator.nextTyped(),
                email,
                passwordHasher.hash(rawPassword),
                displayName,
                Set.of(UserAccount.DEFAULT_ROLE),
                clock.instant());
        userRepository.insert(user);

        return issueNewSession(user).result();
    }

    /** Not read-only: issuing a session persists a new refresh_tokens row. */
    public AuthResult login(String email, String rawPassword) {
        UserAccount user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (!passwordHasher.matches(rawPassword, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueNewSession(user).result();
    }

    /**
     * Rotates a refresh token: the presented token is revoked and a new
     * one issued in its place. If the presented token was already
     * revoked (meaning it was already rotated once before, and this is a
     * second use of the same plaintext value - only possible if it
     * leaked), every live token for that user is revoked and the caller
     * is forced to log in again everywhere.
     */
    public AuthResult refresh(String presentedToken) {
        String presentedHash = refreshTokenGenerator.hash(presentedToken);
        RefreshTokenRecord record = refreshTokenRepository.findByTokenHash(presentedHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("unknown token"));

        if (record.isRevoked()) {
            refreshTokenRepository.revokeAllForUser(record.userId(), clock.instant());
            throw new InvalidRefreshTokenException("reuse of a rotated token - all sessions revoked");
        }
        if (record.isExpired(clock.instant())) {
            throw new InvalidRefreshTokenException("expired");
        }

        UserAccount user = userRepository.findById(record.userId())
                .orElseThrow(() -> new InvalidRefreshTokenException("user no longer exists"));

        IssuedSession issued = issueNewSession(user);
        refreshTokenRepository.revoke(record.id(), clock.instant(), Optional.of(issued.refreshTokenId()));
        return issued.result();
    }

    public void logout(String presentedToken) {
        String presentedHash = refreshTokenGenerator.hash(presentedToken);
        refreshTokenRepository.findByTokenHash(presentedHash)
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> refreshTokenRepository.revoke(token.id(), clock.instant(), Optional.empty()));
    }

    public UserAccount requireUser(TypedId<UserAccount> id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private IssuedSession issueNewSession(UserAccount user) {
        String accessToken = jwtIssuer.issueAccessToken(user, properties.getAccessTokenTtl());

        String refreshPlaintext = refreshTokenGenerator.generate();
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getRefreshTokenTtl());
        String refreshTokenId = idGenerator.next();

        refreshTokenRepository.save(new RefreshTokenRecord(
                refreshTokenId,
                user.id(),
                refreshTokenGenerator.hash(refreshPlaintext),
                issuedAt,
                expiresAt,
                Optional.empty(),
                Optional.empty()));

        return new IssuedSession(new AuthResult(user, accessToken, refreshPlaintext, expiresAt), refreshTokenId);
    }

    /** Carries the new refresh token's row id alongside the caller-facing {@link AuthResult} so {@link #refresh} can chain replaced_by_id without any shared mutable state. */
    private record IssuedSession(AuthResult result, String refreshTokenId) {
    }
}
