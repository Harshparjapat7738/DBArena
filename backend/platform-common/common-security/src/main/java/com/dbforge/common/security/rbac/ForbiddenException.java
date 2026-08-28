package com.dbforge.common.security.rbac;

import com.dbforge.common.core.error.DomainException;

import java.util.Map;
import java.util.Set;

public class ForbiddenException extends DomainException {

    public ForbiddenException(Set<String> requiredRoles, boolean requireAll) {
        super("auth.forbidden", 403,
                "Caller lacks " + (requireAll ? "all of" : "any of") + " required role(s): " + requiredRoles,
                Map.of("requiredRoles", requiredRoles, "requireAll", requireAll));
    }
}
