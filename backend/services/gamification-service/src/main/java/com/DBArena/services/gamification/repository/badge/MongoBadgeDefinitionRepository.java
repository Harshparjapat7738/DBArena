package com.DBArena.services.gamification.repository.badge;

import com.DBArena.services.gamification.domain.badge.BadgeDefinition;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.DBArena.services.gamification.repository.badge.BadgeDefinitionDocumentMapper.*;

@Repository
public class MongoBadgeDefinitionRepository implements BadgeDefinitionRepository {

    private final MongoCollection<Document> collection;

    public MongoBadgeDefinitionRepository(MongoCollection<Document> badgeDefinitionsCollection) {
        this.collection = badgeDefinitionsCollection;
    }

    @Override
    public void insert(BadgeDefinition badge) {
        collection.insertOne(toDocument(badge));
    }

    @Override
    public Optional<BadgeDefinition> findBySlug(String slug) {
        Document document = collection.find(Filters.eq(SLUG, slug)).first();
        return Optional.ofNullable(document).map(BadgeDefinitionDocumentMapper::fromDocument);
    }

    @Override
    public List<BadgeDefinition> findAll() {
        List<BadgeDefinition> result = new java.util.ArrayList<>();
        for (Document document : collection.find()) {
            result.add(BadgeDefinitionDocumentMapper.fromDocument(document));
        }
        return result;
    }
}
