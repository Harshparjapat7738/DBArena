package com.dbforge.services.ai.context;

import com.dbforge.services.ai.domain.HintLevel;

import java.util.List;
import java.util.Set;

/**
 * Everything {@link com.dbforge.services.ai.prompt.HintPromptBuilder} is
 * allowed to hand the model - and, by construction, nothing more. There is
 * deliberately no field here for a reference solution or for any dataset
 * rows beyond {@link AiContextBuilder#MAX_SAMPLE_ROWS_PER_ENTITY} per
 * entity (hard rule #5). If a future change needs to add a field, ask
 * whether it violates that rule before adding it, not after.
 */
public record HintContext(
        String problemTitle,
        String statementExcerpt,
        String difficulty,
        Set<String> tags,
        Set<String> allowedEngines,
        List<EntitySummary> entities,
        String learnerQueryExcerpt,
        String learnerNotesExcerpt,
        HintLevel level) {

    public HintContext {
        entities = List.copyOf(entities);
    }
}
