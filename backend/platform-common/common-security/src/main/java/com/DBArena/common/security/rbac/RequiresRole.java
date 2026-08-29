package com.DBArena.common.security.rbac;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enforces role-based access at method entry, before the method body
 * runs. Requires no authenticated user at all to fail closed (403, not
 * 401 - use the plain {@code @CurrentUser AuthenticatedUser} parameter,
 * which 401s, to require "any authenticated user").
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {

    /** Role names; caller needs at least one unless {@link #requireAll()} is set. */
    String[] value();

    /** If true, the caller must hold every listed role, not just one. */
    boolean requireAll() default false;
}
