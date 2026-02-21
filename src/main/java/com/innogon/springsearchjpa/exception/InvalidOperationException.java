package com.innogon.springsearchjpa.exception;

/**
 * Thrown when a search query contains an unsupported operation
 * for a given field type.
 */
public class InvalidOperationException extends SearchQueryException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
