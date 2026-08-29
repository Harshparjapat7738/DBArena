package com.DBArena.services.execution.validation;

import java.util.Optional;

public record ValidationResult(boolean allowed, Optional<String> rejectionReason) {

    public ValidationResult {
        rejectionReason = rejectionReason == null ? Optional.empty() : rejectionReason;
        if (!allowed && rejectionReason.isEmpty()) {
            throw new IllegalArgumentException("a rejected result must carry a reason");
        }
    }

    public static ValidationResult allow() {
        return new ValidationResult(true, Optional.empty());
    }

    public static ValidationResult reject(String reason) {
        return new ValidationResult(false, Optional.of(reason));
    }
}
