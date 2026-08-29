package com.DBArena.services.execution.service;

import com.DBArena.services.execution.domain.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * "Audit logging" (the task brief) - a structured line per status
 * transition, on its own logger name so it can be routed/retained
 * separately from ordinary application logs. The statement text itself is
 * logged truncated (not omitted - an abuse investigation needs to see what
 * was actually run) via MDC fields, riding common-observability's existing
 * JSON logging setup (see {@code logback-json-base.xml}) rather than a
 * bespoke audit-log format.
 */
@Component
public class ExecutionAuditLogger {

    private static final Logger log = LoggerFactory.getLogger("EXECUTION_AUDIT");
    private static final int STATEMENT_LOG_PREVIEW_LENGTH = 200;

    public void log(Execution execution, String event) {
        try {
            MDC.put("executionId", execution.id().value());
            MDC.put("userId", execution.userId().value());
            MDC.put("engine", execution.engine().name());
            MDC.put("datasetSlug", execution.datasetSlug());
            MDC.put("status", execution.status().name());
            MDC.put("statementPreview", preview(execution.statementText()));
            log.info("execution audit: {}", event);
        } finally {
            MDC.clear();
        }
    }

    private static String preview(String statementText) {
        return statementText.length() <= STATEMENT_LOG_PREVIEW_LENGTH
                ? statementText
                : statementText.substring(0, STATEMENT_LOG_PREVIEW_LENGTH) + "...(truncated)";
    }
}
