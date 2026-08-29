package com.DBArena.services.execution.web.dto;

import com.DBArena.engine.spi.EngineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * No credential field, no timeout/row-limit/connection field - every one of
 * those is a server-side {@code ExecutionPolicy} constant, never
 * client-supplied (the task brief's "no client-supplied credentials",
 * applied to policy generally, not just auth). The {@code @Size} cap here
 * is a cheap pre-parse guard; {@code QueryValidator} enforces the real,
 * policy-driven limit against the parsed statement.
 */
public record CreateExecutionRequest(
        @NotNull EngineType engine,
        @NotBlank String datasetSlug,
        String problemSlug,
        @NotBlank @Size(max = 20_000) String statementText) {
}
