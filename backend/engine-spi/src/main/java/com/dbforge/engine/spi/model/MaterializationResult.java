package com.dbforge.engine.spi.model;

import java.time.Instant;
import java.util.Map;

public record MaterializationResult(SessionHandle session, Instant materializedAt, Map<String, Long> rowCountsByEntity) {

    public MaterializationResult {
        rowCountsByEntity = Map.copyOf(rowCountsByEntity);
    }
}
