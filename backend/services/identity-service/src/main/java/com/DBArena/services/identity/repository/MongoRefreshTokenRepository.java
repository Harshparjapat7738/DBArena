package com.DBArena.services.identity.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.identity.domain.RefreshTokenRecord;
import com.DBArena.services.identity.domain.UserAccount;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.DBArena.services.identity.repository.RefreshTokenDocumentMapper.*;

@Repository
public class MongoRefreshTokenRepository implements RefreshTokenRepository {

    private final MongoCollection<Document> refreshTokensCollection;

    public MongoRefreshTokenRepository(MongoCollection<Document> refreshTokensCollection) {
        this.refreshTokensCollection = refreshTokensCollection;
    }

    @Override
    public void save(RefreshTokenRecord token) {
        refreshTokensCollection.insertOne(toDocument(token));
    }

    @Override
    public Optional<RefreshTokenRecord> findByTokenHash(String tokenHash) {
        Document document = refreshTokensCollection.find(Filters.eq(TOKEN_HASH, tokenHash)).first();
        return Optional.ofNullable(document).map(RefreshTokenDocumentMapper::fromDocument);
    }

    @Override
    public void revoke(String tokenId, Instant revokedAt, Optional<String> replacedById) {
        List<Bson> updates = new ArrayList<>();
        updates.add(Updates.set(REVOKED_AT, revokedAt.toEpochMilli()));
        updates.add(replacedById.<Bson>map(id -> Updates.set(REPLACED_BY_ID, id))
                .orElse(Updates.unset(REPLACED_BY_ID)));
        refreshTokensCollection.updateOne(Filters.eq(ID, tokenId), Updates.combine(updates));
    }

    @Override
    public void revokeAllForUser(TypedId<UserAccount> userId, Instant revokedAt) {
        // A multi-document updateMany - each matched document is updated
        // atomically, same guarantee level the equivalent Postgres
        // UPDATE ... WHERE statement had (not a cross-document
        // transaction either way).
        refreshTokensCollection.updateMany(
                Filters.and(Filters.eq(USER_ID, userId.value()), Filters.exists(REVOKED_AT, false)),
                Updates.set(REVOKED_AT, revokedAt.toEpochMilli()));
    }
}
