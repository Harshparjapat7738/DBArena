package com.dbforge.common.security.rbac;

import com.dbforge.common.security.AuthenticatedUser;
import com.dbforge.common.security.context.CurrentUserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * Enforces {@link RequiresRole}. Registered as a bean by
 * {@code CommonSecurityAutoConfiguration}; requires
 * {@code spring-boot-starter-aop} (already a dependency of this module).
 */
@Aspect
public class RoleAuthorizationAspect {

    @Around("@annotation(com.dbforge.common.security.rbac.RequiresRole) "
            + "|| @within(com.dbforge.common.security.rbac.RequiresRole)")
    public Object enforce(ProceedingJoinPoint joinPoint) throws Throwable {
        RequiresRole annotation = resolveAnnotation(joinPoint);
        if (annotation != null) {
            AuthenticatedUser user = CurrentUserContext.get()
                    .orElseThrow(() -> new ForbiddenException(Set.of(annotation.value()), annotation.requireAll()));

            boolean authorized = annotation.requireAll()
                    ? Arrays.stream(annotation.value()).allMatch(user::hasRole)
                    : user.hasAnyRole(annotation.value());

            if (!authorized) {
                throw new ForbiddenException(Set.of(annotation.value()), annotation.requireAll());
            }
        }
        return joinPoint.proceed();
    }

    private static RequiresRole resolveAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresRole onMethod = method.getAnnotation(RequiresRole.class);
        if (onMethod != null) {
            return onMethod;
        }
        return joinPoint.getTarget().getClass().getAnnotation(RequiresRole.class);
    }
}
