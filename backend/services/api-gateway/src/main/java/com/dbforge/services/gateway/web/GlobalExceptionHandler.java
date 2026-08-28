package com.dbforge.services.gateway.web;

import com.dbforge.common.core.error.DomainException;
import com.dbforge.common.security.web.CurrentUserArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Same shape as identity-service's handler - lift into a shared common-web module once a third HTTP service needs it. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(ex.status()), ex.getMessage());
        problem.setProperty("code", ex.code());
        ex.extensions().forEach(problem::setProperty);
        return problem;
    }

    @ExceptionHandler(CurrentUserArgumentResolver.UnauthenticatedException.class)
    public ProblemDetail handleUnauthenticated(CurrentUserArgumentResolver.UnauthenticatedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setProperty("code", "auth.unauthenticated");
        return problem;
    }
}
