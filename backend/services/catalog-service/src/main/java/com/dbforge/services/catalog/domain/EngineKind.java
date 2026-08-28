package com.dbforge.services.catalog.domain;

/**
 * The three engines a dataset is materialized into (root CLAUDE.md's core
 * premise). Deliberately redeclared here rather than shared with
 * engine-spi: engine-spi (B01) has no such enum yet - its DatasetDescriptor
 * is a documented placeholder pending the real CDM design (B02/docs/02) -
 * and hard rule #1 forbids engine-spi from depending on anything that
 * could pull Spring in via a back door. When B02/B03 land a real,
 * CDM-owned engine enum, this one should be deleted in favor of it rather
 * than kept as a second source of truth - flagged in this milestone's
 * Session Log "Carried forward".
 */
public enum EngineKind {
    POSTGRES,
    MYSQL,
    MONGODB
}
