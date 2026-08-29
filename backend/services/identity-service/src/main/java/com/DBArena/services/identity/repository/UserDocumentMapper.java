package com.DBArena.services.identity.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.identity.domain.UserAccount;
import org.bson.Document;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link UserAccount} &lt;-&gt; {@link Document}. Kept as a pure,
 * dependency-free mapper (no Mongo driver calls) so it is trivially
 * unit-testable without a running database - same split
 * catalog-service's {@code ProblemDocumentMapper} uses.
 *
 * <p>{@code createdAt} is stored as a plain epoch-millis {@code long},
 * never a BSON {@code Date} (hard rule #9) - {@link UserAccount} itself
 * still holds an {@link Instant}; the millis conversion happens only at
 * this boundary, mirroring how the old JDBC repository converted to/from
 * {@code java.sql.Timestamp} at the JDBC boundary and nowhere else.
 *
 * <p>Roles are embedded as a plain string array on the user document
 * (no separate {@code user_roles} collection) - see the Mongock
 * changelog's Javadoc for why that's safe without a transaction.
 */
public final class UserDocumentMapper {

    static final String ID = "_id";
    static final String EMAIL = "email";
    static final String PASSWORD_HASH = "passwordHash";
    static final String DISPLAY_NAME = "displayName";
    static final String ROLES = "roles";
    static final String CREATED_AT = "createdAt";

    private UserDocumentMapper() {
    }

    public static Document toDocument(UserAccount user) {
        return new Document()
                .append(ID, user.id().value())
                .append(EMAIL, user.email())
                .append(PASSWORD_HASH, user.passwordHash())
                .append(DISPLAY_NAME, user.displayName())
                .append(ROLES, List.copyOf(user.roles()))
                // epoch millis, never a BSON Date - hard rule #9.
                .append(CREATED_AT, user.createdAt().toEpochMilli());
    }

    public static UserAccount fromDocument(Document document) {
        Set<String> roles = new LinkedHashSet<>(document.getList(ROLES, String.class, List.of()));
        return new UserAccount(
                TypedId.of(document.getString(ID)),
                document.getString(EMAIL),
                document.getString(PASSWORD_HASH),
                document.getString(DISPLAY_NAME),
                roles,
                Instant.ofEpochMilli(document.getLong(CREATED_AT)));
    }
}
