package com.dbforge.common.web;

import com.dbforge.common.core.error.DomainException;
import com.dbforge.common.security.web.CurrentUserArgumentResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates every exception a service's web layer can surface into RFC
 * 7807 {@code application/problem+json}, per root CLAUDE.md conventions.
 * Shared across every HTTP service in the reactor instead of copy-pasted
 * per service (see this module's Javadoc/description for the history).
 */
@RestControllerAdvice
public class GlobalProblemExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalProblemExceptionHandler.class);

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

    /**
     * Closes a real gap flagged in M13's Session Log: {@code PageRequest}'s
     * compact constructor (and other input-shape guards across the
     * reactor) throw a plain {@link IllegalArgumentException} for a
     * malformed request - e.g. an out-of-range {@code ?limit=} - which no
     * per-service handler mapped, so it surfaced as a raw, non-RFC-7807
     * 500. Mapped to 400 here instead.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                ex.getMessage() == null ? "Invalid request" : ex.getMessage());
        problem.setProperty("code", "request.invalid_argument");
        return problem;
    }

    /**
     * Last-resort fallback so an unexpected bug still returns RFC 7807
     * shape instead of Spring Boot's default error body. Never leaks the
     * exception message or stack trace to the client - only to the
     * server log, keyed by the correlation id common-observability's
     * filter already attaches to the MDC.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        problem.setProperty("code", "internal.unexpected");
        return problem;
    }
}
