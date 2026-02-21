package com.innogon.springsearchjpa.strategies;

import com.innogon.springsearchjpa.SearchOperation;

import java.util.Arrays;

public class EnumStrategy implements ParsingStrategy {

    @Override
    public Object parse(String value, Class<?> fieldClass) {
        if (SearchOperation.NULL.equals(value)) return value;
        return Arrays.stream(fieldClass.getEnumConstants())
                .filter(e -> ((Enum<?>) e).name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No enum constant " + fieldClass.getSimpleName() + "." + value));
    }
}
