package com.DBArena.common.security.web;

import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.common.security.context.CurrentUserContext;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Optional<AuthenticatedUser> user = CurrentUserContext.get();

        if (parameter.getParameterType().equals(Optional.class)) {
            return user;
        }
        return user.orElseThrow(() -> new UnauthenticatedException(
                "Endpoint requires an authenticated caller but no valid access token was presented"));
    }

    public static final class UnauthenticatedException extends RuntimeException {
        public UnauthenticatedException(String message) {
            super(message);
        }
    }
}
