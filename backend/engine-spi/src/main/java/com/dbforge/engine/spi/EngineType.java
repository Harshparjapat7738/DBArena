package com.dbforge.engine.spi;

/** The set of engines a dataset can be materialized into. Adding a value here is an "adding a new engine" change - see backend/CLAUDE.md. */
public enum EngineType {
    POSTGRES,
    MONGODB
}
