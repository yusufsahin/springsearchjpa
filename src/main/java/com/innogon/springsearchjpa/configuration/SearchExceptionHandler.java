package com.innogon.springsearchjpa.configuration;

import com.innogon.springsearchjpa.exception.SearchQueryException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * Catches {@link SearchQueryException} thrown during JPA query execution
 * (from {@code toPredicate()}) and returns HTTP 400 instead of 500.
 * <p>
 * Parse-time exceptions are already handled by
 * {@link SearchSpecificationResolver}; this handler covers runtime
 * validation errors (invalid field names, unsupported operations, etc.)
 * that can only be detected when the specification is evaluated.
 * <p>
 * Disable with {@code spring-search.exception-handler.enabled=false}.
 */
@ControllerAdvice
public class SearchExceptionHandler {

    @ExceptionHandler(SearchQueryException.class)
    public ResponseEntity<Map<String, String>> handleSearchException(SearchQueryException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
    }
}
