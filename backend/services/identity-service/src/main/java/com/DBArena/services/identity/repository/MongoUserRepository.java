package com.DBArena.services.identity.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.identity.domain.UserAccount;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.DBArena.services.identity.repository.UserDocumentMapper.*;

@Repository
public class MongoUserRepository implements UserRepository {

    // Matches the unique index's collation (see the Mongock changelog) so
    // an application-level lookup and the DB-level uniqueness constraint
    // agree on what "the same email" means.
    private static final Collation CASE_INSENSITIVE =
            Collation.builder().locale("en").collationStrength(CollationStrength.SECONDARY).build();

    private final MongoCollection<Document> usersCollection;

    public MongoUserRepository(MongoCollection<Document> usersCollection) {
        this.usersCollection = usersCollection;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        Document document = usersCollection.find(Filters.eq(EMAIL, email))
                .collation(CASE_INSENSITIVE)
                .first();
        return Optional.ofNullable(document).map(UserDocumentMapper::fromDocument);
    }

    @Override
    public Optional<UserAccount> findById(TypedId<UserAccount> id) {
        Document document = usersCollection.find(Filters.eq(ID, id.value())).first();
        return Optional.ofNullable(document).map(UserDocumentMapper::fromDocument);
    }

    @Override
    public boolean existsByEmail(String email) {
        return usersCollection.countDocuments(Filters.eq(EMAIL, email), new com.mongodb.client.model.CountOptions()
                .collation(CASE_INSENSITIVE)) > 0;
    }

    @Override
    public void insert(UserAccount user) {
        // A single document write - atomic by itself, no transaction
        // needed even though the old Postgres version had a second table
        // (user_roles) to insert alongside it. See UserDocumentMapper's
        // Javadoc for why roles are embedded here instead.
        usersCollection.insertOne(toDocument(user));
    }
}
