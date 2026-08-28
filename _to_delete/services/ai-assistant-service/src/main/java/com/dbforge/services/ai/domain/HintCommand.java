package com.dbforge.services.ai.domain;

import java.util.Optional;

/**
 * The already-validated, service-internal shape of a hint request - the
 * web layer's {@code HintRequestBody} is converted into this before
 * anything else touches it.
 */
public record HintCommand(
        String problemSlug,
        String learnerQuery,
        Optional<String> errorOrResultText,
        HintLevel level) {

    public HintCommand {
        if (problemSlug == null || problemSlug.isBlank()) {
            throw new IllegalArgumentException("problemSlug must not be blank");
        }
        if (learnerQuery == null || learnerQuery.isBlank()) {
            throw new IllegalArgumentException("learnerQuery must not be blank");
        }
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
        errorOrResultText = errorOrResultText == null ? Optional.empty() : errorOrResultText;
    }
}
