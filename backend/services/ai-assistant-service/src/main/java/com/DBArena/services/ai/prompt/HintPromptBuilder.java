package com.DBArena.services.ai.prompt;

import com.DBArena.services.ai.context.ColumnSummary;
import com.DBArena.services.ai.context.EntitySummary;
import com.DBArena.services.ai.context.HintContext;
import com.DBArena.services.ai.domain.HintLevel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Builds a compact system + user prompt from a {@link HintContext}. "Compact"
 * is a deliberate design goal, not just a nice-to-have (the human's own
 * words: "prompt must be compact and output must be within the output
 * range") - every section here is a dense, single-line-per-item rendering,
 * not prose, so the whole context stays small even for a dataset with
 * several entities.
 *
 * <p>The system prompt is the actual enforcement point for "never reveal
 * the reference solution": there is no reference solution anywhere in
 * {@link HintContext} for the model to reveal (this system has none built
 * yet - B12/problem-validator is what would introduce one), but the
 * instruction is written to hold even after that changes, so nobody has to
 * remember to add it later.
 */
@Component
public class HintPromptBuilder {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are a terse database-learning tutor embedded in a practice platform.
            Give a %s-level hint for the CURRENT level only - never skip ahead to a full solution.
            Rules, always:
            - Never write a complete, directly runnable final query. Sketches/fragments only at NEAR_MISS level.
            - Never claim to know or reveal a "reference solution" - none is provided to you; do not invent one.
            - Reference ONLY the schema/sample rows/query given below - never invent columns or data.
            - Plain text, no markdown headers, no code fences unless quoting a short fragment.
            - Hard limit: %d words. Be dense, not padded.
            Level guide: CONCEPT = name the relevant idea only. APPROACH = describe the shape of a solution in prose, no code. NEAR_MISS = point at what's specifically wrong/missing in the learner's own query, short fragment allowed, still not the full answer.
            """;

    private static final int TARGET_WORD_LIMIT = 120;

    public String buildSystemPrompt(HintLevel level) {
        return SYSTEM_PROMPT_TEMPLATE.formatted(level.name(), TARGET_WORD_LIMIT);
    }

    public String buildUserPrompt(HintContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("PROBLEM: ").append(context.problemTitle())
                .append(" [").append(context.difficulty()).append("]\n");
        if (!context.tags().isEmpty()) {
            sb.append("TAGS: ").append(String.join(",", context.tags())).append('\n');
        }
        sb.append("STATEMENT: ").append(context.statementExcerpt()).append('\n');

        if (!context.entities().isEmpty()) {
            sb.append("SCHEMA:\n");
            for (EntitySummary entity : context.entities()) {
                sb.append(renderSchemaLine(entity)).append('\n');
                for (Map<String, String> row : entity.sampleRows()) {
                    sb.append("  sample: ").append(row).append('\n');
                }
            }
        }

        sb.append("LEARNER_QUERY:\n").append(context.learnerQueryExcerpt()).append('\n');
        if (context.learnerNotesExcerpt() != null && !context.learnerNotesExcerpt().isBlank()) {
            sb.append("LEARNER_ERROR_OR_RESULT:\n").append(context.learnerNotesExcerpt()).append('\n');
        }
        sb.append("REQUESTED_LEVEL: ").append(context.level().name()).append('\n');
        sb.append("Give the hint now.");
        return sb.toString();
    }

    private String renderSchemaLine(EntitySummary entity) {
        StringJoiner columns = new StringJoiner(", ");
        for (ColumnSummary column : entity.columns()) {
            String flags = (column.primaryKey() ? " PK" : "") + (column.nullable() ? " NULL" : " NOT NULL");
            columns.add(column.name() + ":" + column.type() + flags);
        }
        return entity.name() + "(" + columns + ")";
    }
}
