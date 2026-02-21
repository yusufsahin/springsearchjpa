package com.innogon.springsearchjpa.exception;

/**
 * Thrown when a search query contains a value that cannot be parsed
 * for the target field type.
 */
public class InvalidValueException extends SearchQueryException {

    public InvalidValueException(String message) {
        super(message);
    }

    public InvalidValueException(String message, Throwable cause) {
        super(message, cause);
    }
}
