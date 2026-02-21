package com.innogon.springsearchjpa.configuration;

import com.innogon.springsearchjpa.exception.SearchQueryException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * Catches {@link SearchQueryException} thrown during JPA query execution
 * (from {@code toPredicate()}) and returns HTTP 400 instead of 500.
 * <p>
 * Disable with {@code spring-search.exception-handler.enabled=false}.
 */
@ControllerAdvice
public class SearchExceptionHandler {

    @ExceptionHandler(SearchQueryException.class)
    public ResponseEntity<Map<String, String>> handleSearchException(SearchQueryException ex) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", ex.getMessage()));
    }
}
