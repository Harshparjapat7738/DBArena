package com.DBArena.services.execution.validation;

import java.util.Set;

/**
 * The deny-lists {@link PostgresSqlQueryValidator} checks a parsed
 * statement's AST against - never the raw SQL text (hard rule #4). Both
 * lists are deliberately conservative (biased toward blocking) since this
 * is "safe practice SQL only" (the task brief) - a legitimate practice
 * problem needing one of these is expected to be vanishingly rare, and a
 * false rejection is far cheaper than a sandbox escape or a resource-
 * exhaustion vector.
 */
final class DeniedSqlSurface {

    private DeniedSqlSurface() {
    }

    /**
     * Function names (last component of a possibly schema-qualified call,
     * lowercase) that are never allowed: artificial delay / DoS
     * ({@code pg_sleep*}), filesystem access, process/session control,
     * cross-database or network egress ({@code dblink*}, {@code postgres_fdw}
     * helpers), large-object I/O, and config mutation.
     */
    static final Set<String> DENIED_FUNCTIONS = Set.of(
            "pg_sleep", "pg_sleep_for", "pg_sleep_until",
            "pg_read_file", "pg_read_binary_file", "pg_ls_dir", "pg_ls_logdir", "pg_ls_waldir",
            "pg_stat_file", "pg_stat_reset",
            "lo_import", "lo_export", "lo_create", "lo_unlink", "lo_read", "lo_write",
            "dblink", "dblink_connect", "dblink_connect_u", "dblink_exec",
            "pg_terminate_backend", "pg_cancel_backend", "pg_reload_conf", "pg_rotate_logfile",
            "set_config", "pg_backend_pid", "pg_advisory_lock", "pg_advisory_unlock",
            "query_to_xml", "xmlparse", "xpath");

    /** Schema-qualifier names a referenced table/view must never sit in - reconnaissance and metadata access. */
    static final Set<String> DENIED_SCHEMAS = Set.of("pg_catalog", "information_schema", "pg_toast");
}
