package com.dbforge.services.catalog.repository;

import com.dbforge.common.core.id.TypedId;
import com.dbforge.services.catalog.domain.Difficulty;
import com.dbforge.engine.spi.EngineType;
import com.dbforge.services.catalog.domain.Problem;
import org.bson.Document;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link Problem} &lt;-&gt; {@link Document}. Kept as a pure, dependency-free
 * mapper (no Mongo driver calls) so it is trivially unit-testable without
 * a running database.
 */
public final class ProblemDocumentMapper {

    static final String ID = "_id";
    static final String SLUG = "slug";
    static final String TITLE = "title";
    static final String STATEMENT_MARKDOWN = "statementMarkdown";
    static final String DIFFICULTY = "difficulty";
    static final String TAGS = "tags";
    static final String ALLOWED_ENGINES = "allowedEngines";
    static final String DATASET_SLUG = "datasetSlug";
    static final String PUBLISHED = "published";
    static final String CREATED_AT = "createdAt";
    static final String UPDATED_AT = "updatedAt";

    private ProblemDocumentMapper() {
    }

    public static Document toDocument(Problem problem) {
        return new Document()
                .append(ID, problem.id().value())
                .append(SLUG, problem.slug())
                .append(TITLE, problem.title())
                .append(STATEMENT_MARKDOWN, problem.statementMarkdown())
                .append(DIFFICULTY, problem.difficulty().name())
                .append(TAGS, List.copyOf(problem.tags()))
                .append(ALLOWED_ENGINES, problem.allowedEngines().stream().map(Enum::name).toList())
                .append(DATASET_SLUG, problem.datasetSlug())
                .append(PUBLISHED, problem.published())
                // epoch millis, never a BSON Date - hard rule #9.
                .append(CREATED_AT, problem.createdAtEpochMillis())
                .append(UPDATED_AT, problem.updatedAtEpochMillis());
    }

    public static Problem fromDocument(Document document) {
        Set<String> tags = new LinkedHashSet<>(document.getList(TAGS, String.class, List.of()));
        Set<EngineType> engines = document.getList(ALLOWED_ENGINES, String.class, List.of()).stream()
                .map(EngineType::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new Problem(
                TypedId.of(document.getString(ID)),
                document.getString(SLUG),
                document.getString(TITLE),
                document.getString(STATEMENT_MARKDOWN),
                Difficulty.valueOf(document.getString(DIFFICULTY)),
                tags,
                engines,
                document.getString(DATASET_SLUG),
                Boolean.TRUE.equals(document.getBoolean(PUBLISHED)),
                document.getLong(CREATED_AT),
                document.getLong(UPDATED_AT));
    }
}
