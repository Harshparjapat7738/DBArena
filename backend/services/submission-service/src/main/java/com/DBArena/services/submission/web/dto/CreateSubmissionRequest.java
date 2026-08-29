package com.DBArena.services.submission.web.dto;

import com.DBArena.engine.spi.EngineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code queryText} is capped, not open-ended - this collection must never
 * be asked to hold something large (a raw result set, say); see
 * {@link com.DBArena.services.submission.domain.Submission}'s Javadoc.
 */
public record CreateSubmissionRequest(
        @NotBlank String problemSlug,
        @NotNull EngineType engine,
        @NotBlank @Size(max = 20_000) String queryText) {
}
