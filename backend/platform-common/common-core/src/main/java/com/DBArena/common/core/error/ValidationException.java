package com.dbforge.common.core.error;

import java.util.List;
import java.util.Map;

public class ValidationException extends DomainException {

    private final List<FieldViolation> violations;

    public ValidationException(List<FieldViolation> violations) {
        super("request.invalid", 422, summarize(violations),
                Map.of("violations", violations));
        this.violations = List.copyOf(violations);
    }

    public List<FieldViolation> violations() {
        return violations;
    }

    private static String summarize(List<FieldViolation> violations) {
        if (violations == null || violations.isEmpty()) {
            throw new IllegalArgumentException("violations must not be empty");
        }
        return violations.size() + " validation error(s), first: " + violations.get(0).field()
                + " - " + violations.get(0).message();
    }
}
