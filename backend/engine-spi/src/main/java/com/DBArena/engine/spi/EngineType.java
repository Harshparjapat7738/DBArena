package com.DBArena.engine.spi;

/**
 * The set of engines a dataset can be materialized into. Adding a value
 * here is an "adding a new engine" change - see backend/CLAUDE.md.
 *
 * <p>MYSQL added during the audit pass that also deleted
 * catalog-service's locally-redeclared {@code EngineKind} enum in favor
 * of this one (three Session Log entries - M13, M02, M03 - flagged that
 * duplication as pending exactly this, once B02/B03 landed a real
 * CDM-owned engine enum; B02/B03 are now done). A full adapter
 * ({@code engine-adapters/adapter-mysql}) and type mapper
 * ({@code com.DBArena.engine.spi.typemap.MySqlTypeMapper}) now exist too,
 * built outside the milestone table's numeric order on the human's
 * explicit instruction - see backend/CLAUDE.md's Session Log for that
 * milestone. Only MONGODB has no adapter yet (B05, still not started).
 */
public enum EngineType {
    POSTGRES,
    MYSQL,
    MONGODB
}
