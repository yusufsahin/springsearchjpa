package com.innogon.springsearchjpa.strategies;

import com.innogon.springsearchjpa.SearchOperation;

import java.util.UUID;

public class UUIDStrategy implements ParsingStrategy {

    @Override
    public Object parse(String value, Class<?> fieldClass) {
        if (SearchOperation.NULL.equals(value)) return value;
        return UUID.fromString(value);
    }
}
