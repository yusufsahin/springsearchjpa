package com.innogon.springsearchjpa.strategies;

import com.innogon.springsearchjpa.SearchOperation;

public class BooleanStrategy implements ParsingStrategy {

    @Override
    public Object parse(String value, Class<?> fieldClass) {
        if (SearchOperation.NULL.equals(value)) return value;
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IllegalArgumentException("Invalid boolean value: " + value
                + ". Expected 'true' or 'false'.");
    }
}
