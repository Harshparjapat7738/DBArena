package com.dbforge.services.identity.repository;

import com.dbforge.common.core.id.TypedId;
import com.dbforge.services.identity.domain.RefreshTokenRecord;
import com.dbforge.services.identity.domain.UserAccount;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcRefreshTokenRepository implements RefreshTokenRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcRefreshTokenRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(RefreshTokenRecord token) {
        jdbc.update(
                "INSERT INTO refresh_tokens (id, user_id, token_hash, issued_at, expires_at, revoked_at, replaced_by_id) "
                        + "VALUES (:id, :userId, :tokenHash, :issuedAt, :expiresAt, :revokedAt, :replacedById)",
                new MapSqlParameterSource()
                        .addValue("id", token.id())
                        .addValue("userId", token.userId().value())
                        .addValue("tokenHash", token.tokenHash())
                        .addValue("issuedAt", Timestamp.from(token.issuedAt()))
                        .addValue("expiresAt", Timestamp.from(token.expiresAt()))
                        .addValue("revokedAt", token.revokedAt().map(Timestamp::from).orElse(null))
                        .addValue("replacedById", token.replacedById().orElse(null)));
    }

    @Override
    public Optional<RefreshTokenRecord> findByTokenHash(String tokenHash) {
        List<RefreshTokenRecord> rows = jdbc.query(
                "SELECT id, user_id, token_hash, issued_at, expires_at, revoked_at, replaced_by_id "
                        + "FROM refresh_tokens WHERE token_hash = :tokenHash",
                new MapSqlParameterSource("tokenHash", tokenHash),
                this::mapRow);
        return rows.stream().findFirst();
    }

    @Override
    public void revoke(String tokenId, Instant revokedAt, Optional<String> replacedById) {
        jdbc.update(
                "UPDATE refresh_tokens SET revoked_at = :revokedAt, replaced_by_id = :replacedById WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("revokedAt", Timestamp.from(revokedAt))
                        .addValue("replacedById", replacedById.orElse(null))
                        .addValue("id", tokenId));
    }

    @Override
    public void revokeAllForUser(TypedId<UserAccount> userId, Instant revokedAt) {
        jdbc.update(
                "UPDATE refresh_tokens SET revoked_at = :revokedAt "
                        + "WHERE user_id = :userId AND revoked_at IS NULL",
                new MapSqlParameterSource()
                        .addValue("revokedAt", Timestamp.from(revokedAt))
                        .addValue("userId", userId.value()));
    }

    private RefreshTokenRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp revokedAt = rs.getTimestamp("revoked_at");
        String replacedById = rs.getString("replaced_by_id");
        return new RefreshTokenRecord(
                rs.getString("id"),
                TypedId.of(rs.getString("user_id")),
                rs.getString("token_hash"),
                rs.getTimestamp("issued_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(),
                Optional.ofNullable(revokedAt).map(Timestamp::toInstant),
                Optional.ofNullable(replacedById));
    }
}
