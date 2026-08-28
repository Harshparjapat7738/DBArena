package com.dbforge.engine.spi.cdm;

import com.dbforge.common.core.error.FieldViolation;

import java.util.List;

public record CdmValidationResult(List<FieldViolation> violations) {

    public CdmValidationResult {
        violations = List.copyOf(violations);
    }

    public boolean valid() {
        return violations.isEmpty();
    }

    public static CdmValidationResult ok() {
        return new CdmValidationResult(List.of());
    }
}
