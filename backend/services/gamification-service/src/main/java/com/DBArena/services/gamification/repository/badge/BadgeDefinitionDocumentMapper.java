package com.DBArena.services.gamification.repository.badge;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.gamification.domain.badge.BadgeDefinition;
import com.DBArena.services.gamification.domain.badge.BadgeTier;
import org.bson.Document;

public final class BadgeDefinitionDocumentMapper {

    static final String ID = "_id";
    static final String SLUG = "slug";
    static final String NAME = "name";
    static final String DESCRIPTION = "description";
    static final String ICON = "icon";
    static final String TIER = "tier";

    private BadgeDefinitionDocumentMapper() {
    }

    public static Document toDocument(BadgeDefinition badge) {
        return new Document()
                .append(ID, badge.id().value())
                .append(SLUG, badge.slug())
                .append(NAME, badge.name())
                .append(DESCRIPTION, badge.description())
                .append(ICON, badge.icon())
                .append(TIER, badge.tier().name());
    }

    public static BadgeDefinition fromDocument(Document document) {
        return new BadgeDefinition(
                TypedId.of(document.getString(ID)),
                document.getString(SLUG),
                document.getString(NAME),
                document.getString(DESCRIPTION),
                document.getString(ICON),
                BadgeTier.valueOf(document.getString(TIER)));
    }
}
