package com.DBArena.common.security.rbac;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.common.security.context.CurrentUserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleAuthorizationAspectTest {

    private final RoleAuthorizationAspect aspect = new RoleAuthorizationAspect();

    interface Sample {
        @RequiresRole("author")
        String authorOnly();

        @RequiresRole(value = {"author", "admin"}, requireAll = true)
        String needsBoth();
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void allowsCallWhenUserHasRequiredRole() throws Throwable {
        CurrentUserContext.set(user("author"));
        ProceedingJoinPoint jp = joinPointFor("authorOnly", "ok");

        assertThat(aspect.enforce(jp)).isEqualTo("ok");
    }

    @Test
    void deniesCallWhenUserLacksRole() throws Throwable {
        CurrentUserContext.set(user("learner"));
        ProceedingJoinPoint jp = joinPointFor("authorOnly", "ok");

        assertThatThrownBy(() -> aspect.enforce(jp)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deniesCallWhenNoUserIsBound() {
        ProceedingJoinPoint jp = joinPointFor("authorOnly", "ok");

        assertThatThrownBy(() -> aspect.enforce(jp)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireAllNeedsEveryRole() throws Throwable {
        CurrentUserContext.set(user("author"));
        ProceedingJoinPoint jp = joinPointFor("needsBoth", "ok");

        assertThatThrownBy(() -> aspect.enforce(jp)).isInstanceOf(ForbiddenException.class);

        CurrentUserContext.set(user("author", "admin"));
        assertThat(aspect.enforce(jp)).isEqualTo("ok");
    }

    private static AuthenticatedUser user(String... roles) {
        return new AuthenticatedUser(TypedId.of("01J000USER"), Set.of(roles), "access");
    }

    private static ProceedingJoinPoint joinPointFor(String methodName, Object returnValue) {
        try {
            Method method = Sample.class.getMethod(methodName);
            MethodSignature signature = Mockito.mock(MethodSignature.class);
            Mockito.when(signature.getMethod()).thenReturn(method);

            ProceedingJoinPoint jp = Mockito.mock(ProceedingJoinPoint.class);
            Mockito.when(jp.getSignature()).thenReturn(signature);
            Mockito.when(jp.proceed()).thenReturn(returnValue);
            return jp;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
            // ProceedingJoinPoint#proceed() declares a checked Throwable per AspectJ's API,
            // but Mockito.when(...) never actually invokes it here - only stubs the return
            // value - so this can't happen in practice. Satisfies the compiler, not a real path.
            throw new RuntimeException(e);
        }
    }
}
