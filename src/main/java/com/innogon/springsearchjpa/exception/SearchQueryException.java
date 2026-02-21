package com.innogon.springsearchjpa.exception;

/**
 * Base exception for all search query related errors.
 * Allows library consumers to catch search errors independently of HTTP layer.
 */
public class SearchQueryException extends RuntimeException {

    public SearchQueryException(String message) {
        super(message);
    }

    public SearchQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
