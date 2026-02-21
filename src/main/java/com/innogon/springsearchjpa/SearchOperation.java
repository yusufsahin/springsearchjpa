package com.innogon.springsearchjpa;

public enum SearchOperation {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    STARTS_WITH,
    ENDS_WITH,
    CONTAINS,
    DOESNT_START_WITH,
    DOESNT_END_WITH,
    DOESNT_CONTAIN,
    GREATER_THAN_EQUALS,
    LESS_THAN_EQUALS,
    IN_ARRAY,
    NOT_IN_ARRAY,
    IS,
    IS_NOT;

    public static final String ZERO_OR_MORE_REGEX = "*";
    public static final String EMPTY = "EMPTY";
    public static final String NULL = "NULL";

    /**
     * Parse a string operator token into a SearchOperation.
     *
     * @param input operation as string
     * @return The matching operation or null if not recognized
     */
    public static SearchOperation getSimpleOperation(String input) {
        return switch (input) {
            case ":" -> EQUALS;
            case "!" -> NOT_EQUALS;
            case ">" -> GREATER_THAN;
            case "<" -> LESS_THAN;
            case ">:" -> GREATER_THAN_EQUALS;
            case "<:" -> LESS_THAN_EQUALS;
            default -> null;
        };
    }
}
