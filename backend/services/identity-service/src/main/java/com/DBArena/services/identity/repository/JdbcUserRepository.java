package com.dbforge.services.identity.repository;

import com.dbforge.common.core.id.TypedId;
import com.dbforge.services.identity.domain.UserAccount;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class JdbcUserRepository implements UserRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcUserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        List<UserAccount> rows = jdbc.query(
                "SELECT id, email, password_hash, display_name, created_at FROM users WHERE lower(email) = lower(:email)",
                new MapSqlParameterSource("email", email),
                this::mapUserWithoutRoles);
        return rows.stream().findFirst().map(this::attachRoles);
    }

    @Override
    public Optional<UserAccount> findById(TypedId<UserAccount> id) {
        List<UserAccount> rows = jdbc.query(
                "SELECT id, email, password_hash, display_name, created_at FROM users WHERE id = :id",
                new MapSqlParameterSource("id", id.value()),
                this::mapUserWithoutRoles);
        return rows.stream().findFirst().map(this::attachRoles);
    }

    @Override
    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE lower(email) = lower(:email)",
                new MapSqlParameterSource("email", email),
                Integer.class);
        return count != null && count > 0;
    }

    @Override
    public void insert(UserAccount user) {
        jdbc.update(
                "INSERT INTO users (id, email, password_hash, display_name, created_at) "
                        + "VALUES (:id, :email, :passwordHash, :displayName, :createdAt)",
                new MapSqlParameterSource()
                        .addValue("id", user.id().value())
                        .addValue("email", user.email())
                        .addValue("passwordHash", user.passwordHash())
                        .addValue("displayName", user.displayName())
                        .addValue("createdAt", Timestamp.from(user.createdAt())));

        if (!user.roles().isEmpty()) {
            MapSqlParameterSource[] roleParams = user.roles().stream()
                    .map(role -> new MapSqlParameterSource()
                            .addValue("userId", user.id().value())
                            .addValue("role", role))
                    .toArray(MapSqlParameterSource[]::new);
            jdbc.batchUpdate("INSERT INTO user_roles (user_id, role) VALUES (:userId, :role)", roleParams);
        }
    }

    private UserAccount mapUserWithoutRoles(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new UserAccount(
                TypedId.of(rs.getString("id")),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("display_name"),
                Set.of(),
                rs.getTimestamp("created_at").toInstant());
    }

    private UserAccount attachRoles(UserAccount withoutRoles) {
        List<String> roles = jdbc.queryForList(
                "SELECT role FROM user_roles WHERE user_id = :userId",
                new MapSqlParameterSource("userId", withoutRoles.id().value()),
                String.class);
        return new UserAccount(
                withoutRoles.id(),
                withoutRoles.email(),
                withoutRoles.passwordHash(),
                withoutRoles.displayName(),
                Set.copyOf(roles),
                withoutRoles.createdAt());
    }
}
