package com.dbforge.services.ai.web;

import com.dbforge.common.core.error.DomainException;
import com.dbforge.common.security.web.CurrentUserArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Same RFC 7807 shape as identity-service's, api-gateway's, and
 * catalog-service's handlers of the same name - this is now the FOURTH
 * verbatim copy. catalog-service's own Session Log already flagged the
 * third copy as "overdue" for extraction into a shared {@code common-web}
 * module; this milestone did not do that extraction either (see Carried
 * forward - it would have meant touching three already-shipped services
 * to retrofit a shared dependency, which is more than a "one milestone,
 * no side quests" session should take on). Do it the next time a fifth
 * HTTP service needs this, not a sixth copy-paste.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(ex.status()), ex.getMessage());
        problem.setProperty("code", ex.code());
        ex.extensions().forEach(problem::setProperty);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> violations = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            violations.put(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
                violations.size() + " validation error(s)");
        problem.setProperty("code", "request.invalid");
        problem.setProperty("violations", violations);
        return problem;
    }

    @ExceptionHandler(CurrentUserArgumentResolver.UnauthenticatedException.class)
    public ProblemDetail handleUnauthenticated(CurrentUserArgumentResolver.UnauthenticatedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setProperty("code", "auth.unauthenticated");
        return problem;
    }
}
