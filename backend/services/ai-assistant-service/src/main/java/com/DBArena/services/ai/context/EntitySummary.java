package com.DBArena.services.ai.context;

import java.util.List;
import java.util.Map;

/**
 * {@code sampleRows} holds at most {@link AiContextBuilder#MAX_SAMPLE_ROWS_PER_ENTITY}
 * rows - see that constant's Javadoc for why the cap is hard-coded rather
 * than a field on this record or a constructor parameter.
 */
public record EntitySummary(String name, List<ColumnSummary> columns, List<Map<String, String>> sampleRows) {

    public EntitySummary {
        columns = List.copyOf(columns);
        sampleRows = List.copyOf(sampleRows);
    }
}
