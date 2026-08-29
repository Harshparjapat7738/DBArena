package com.DBArena.services.execution.materializer;

import com.DBArena.common.core.error.NotFoundException;

import java.util.Map;

/** The requested dataset slug has no {@code dataset.yaml} on disk, or it failed to parse/validate. */
public class DatasetLoadException extends NotFoundException {

    public DatasetLoadException(String datasetSlug, String detail) {
        super("execution.dataset_unavailable", "Dataset '" + datasetSlug + "' is not available: " + detail,
                Map.of("datasetSlug", datasetSlug));
    }
}
