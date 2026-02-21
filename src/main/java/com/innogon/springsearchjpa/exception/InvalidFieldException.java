package com.innogon.springsearchjpa.exception;

/**
 * Thrown when a search query references a field that is not allowed
 * (blacklisted or not whitelisted).
 */
public class InvalidFieldException extends SearchQueryException {

    public InvalidFieldException(String message) {
        super(message);
    }
}
