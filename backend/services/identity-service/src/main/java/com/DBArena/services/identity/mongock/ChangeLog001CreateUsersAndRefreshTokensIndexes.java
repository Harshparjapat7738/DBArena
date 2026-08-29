package com.DBArena.services.identity.mongock;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

/**
 * Mongock is this platform's Mongo counterpart to Flyway (backend/CLAUDE.md
 * "Conventions") - same reconstructed-from-memory risk note as
 * catalog-service's own ChangeLog001CreateProblemsIndexes; check that
 * class's Javadoc first if {@code mvn verify} fails here.
 *
 * <p>Mirrors the uniqueness/index shape the old Postgres migrations
 * (V1-V3, since deleted) enforced at the DB level:
 * <ul>
 *   <li>{@code users_email_unique_idx ON users (lower(email))} -&gt; a
 *   unique index on {@code email} with a case-insensitive collation
 *   (strength SECONDARY), the Mongo equivalent of a functional index on
 *   {@code lower(email)}.</li>
 *   <li>{@code refresh_tokens_token_hash_unique_idx} -&gt; unique index on
 *   {@code tokenHash}.</li>
 *   <li>{@code refresh_tokens_user_id_idx} -&gt; plain index on
 *   {@code userId}, backing {@code revokeAllForUser}.</li>
 * </ul>
 * Roles are embedded directly in the user document (no separate
 * {@code user_roles} collection) - a single-document write is already
 * atomic in Mongo, so the two-table insert-with-transaction the Postgres
 * version needed has no equivalent requirement here (see AuthService's
 * Javadoc: it no longer needs {@code @Transactional}).
 */
@ChangeUnit(id = "001-create-users-and-refresh-tokens-indexes", order = "001", author = "identity-service")
public class ChangeLog001CreateUsersAndRefreshTokensIndexes {

    private static final String USERS_COLLECTION = "users";
    private static final String REFRESH_TOKENS_COLLECTION = "refresh_tokens";

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        Collation caseInsensitive = Collation.builder()
                .locale("en")
                .collationStrength(CollationStrength.SECONDARY)
                .build();

        var users = mongoDatabase.getCollection(USERS_COLLECTION);
        users.createIndex(Indexes.ascending("email"),
                new IndexOptions().unique(true).collation(caseInsensitive));

        var refreshTokens = mongoDatabase.getCollection(REFRESH_TOKENS_COLLECTION);
        refreshTokens.createIndex(Indexes.ascending("tokenHash"), new IndexOptions().unique(true));
        refreshTokens.createIndex(Indexes.ascending("userId"));
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {
        mongoDatabase.getCollection(USERS_COLLECTION).drop();
        mongoDatabase.getCollection(REFRESH_TOKENS_COLLECTION).drop();
    }
}
