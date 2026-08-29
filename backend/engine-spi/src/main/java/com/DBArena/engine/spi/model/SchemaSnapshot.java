package com.DBArena.engine.spi.model;

import java.util.List;

public record SchemaSnapshot(List<EntitySchema> entities) {

    public SchemaSnapshot {
        entities = List.copyOf(entities);
    }
}
