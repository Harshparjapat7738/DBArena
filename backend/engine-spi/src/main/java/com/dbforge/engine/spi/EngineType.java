package com.dbforge.engine.spi;

/**
 * The set of engines a dataset can be materialized into. Adding a value
 * here is an "adding a new engine" change - see backend/CLAUDE.md.
 *
 * <p>MYSQL added during the audit pass that also deleted
 * catalog-service's locally-redeclared {@code EngineKind} enum in favor
 * of this one (three Session Log entries - M13, M02, M03 - flagged that
 * duplication as pending exactly this, once B02/B03 landed a real
 * CDM-owned engine enum; B02/B03 are now done). No adapter or type
 * mapper exists for MySQL yet (still a real, open gap - see backend
 * CLAUDE.md's milestone table, B04/B05 cover Postgres/Mongo only so far)
 * - this only fixes the enum having fewer values than root CLAUDE.md's
 * stack line, which names MySQL as a target engine, and than the API
 * contract catalog-service already shipped (POSTGRES/MYSQL/MONGODB).
 */
public enum EngineType {
    POSTGRES,
    MYSQL,
    MONGODB
}
