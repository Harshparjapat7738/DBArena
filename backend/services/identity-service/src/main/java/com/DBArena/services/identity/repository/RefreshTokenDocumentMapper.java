package com.DBArena.services.identity.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.identity.domain.RefreshTokenRecord;
import com.DBArena.services.identity.domain.UserAccount;
import org.bson.Document;

import java.time.Instant;
import java.util.Optional;

/**
 * {@link RefreshTokenRecord} &lt;-&gt; {@link Document}. Same split as
 * {@link UserDocumentMapper} - pure, dependency-free, unit-testable
 * without a database. All four timestamp fields are epoch-millis
 * {@code long}s on the wire, never a BSON {@code Date} (hard rule #9).
 */
public final class RefreshTokenDocumentMapper {

    static final String ID = "_id";
    static final String USER_ID = "userId";
    static final String TOKEN_HASH = "tokenHash";
    static final String ISSUED_AT = "issuedAt";
    static final String EXPIRES_AT = "expiresAt";
    static final String REVOKED_AT = "revokedAt";
    static final String REPLACED_BY_ID = "replacedById";

    private RefreshTokenDocumentMapper() {
    }

    public static Document toDocument(RefreshTokenRecord token) {
        Document document = new Document()
                .append(ID, token.id())
                .append(USER_ID, token.userId().value())
                .append(TOKEN_HASH, token.tokenHash())
                .append(ISSUED_AT, token.issuedAt().toEpochMilli())
                .append(EXPIRES_AT, token.expiresAt().toEpochMilli());
        token.revokedAt().ifPresent(revokedAt -> document.append(REVOKED_AT, revokedAt.toEpochMilli()));
        token.replacedById().ifPresent(replacedById -> document.append(REPLACED_BY_ID, replacedById));
        return document;
    }

    public static RefreshTokenRecord fromDocument(Document document) {
        Long revokedAtMillis = document.getLong(REVOKED_AT);
        String replacedById = document.getString(REPLACED_BY_ID);
        return new RefreshTokenRecord(
                document.getString(ID),
                TypedId.of(document.getString(USER_ID)),
                document.getString(TOKEN_HASH),
                Instant.ofEpochMilli(document.getLong(ISSUED_AT)),
                Instant.ofEpochMilli(document.getLong(EXPIRES_AT)),
                Optional.ofNullable(revokedAtMillis).map(Instant::ofEpochMilli),
                Optional.ofNullable(replacedById));
    }
}
