package com.DBArena.common.security.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the resolved {@link com.DBArena.common.security.AuthenticatedUser}
 * as a controller method parameter. Use {@code Optional<AuthenticatedUser>}
 * for an endpoint that behaves differently when unauthenticated;
 * {@code AuthenticatedUser} directly for an endpoint that requires it (a
 * 401 is raised by {@link CurrentUserArgumentResolver} if absent).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
