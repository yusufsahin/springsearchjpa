package com.innogon.springsearchjpa.strategies;

import com.innogon.springsearchjpa.SearchOperation;
import com.innogon.springsearchjpa.exception.InvalidOperationException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

public class CollectionStrategy implements ParsingStrategy {

    @Override
    public Object parse(String value, Class<?> fieldClass) {
        if (SearchOperation.EMPTY.equals(value) || SearchOperation.NULL.equals(value)) {
            return value;
        }
        throw new InvalidOperationException(
                "Collection fields only support IS EMPTY, IS NOT EMPTY, IS NULL, IS NOT NULL. Got value: " + value);
    }

    @Override
    public Predicate buildPredicate(
            CriteriaBuilder builder,
            Path<?> path,
            String fieldName,
            SearchOperation ops,
            Object value) {
        if (ops == SearchOperation.IS) {
            if (SearchOperation.EMPTY.equals(value)) {
                return builder.isEmpty(path.get(fieldName));
            }
            if (SearchOperation.NULL.equals(value)) {
                return builder.isNull(path.get(fieldName));
            }
        }
        if (ops == SearchOperation.IS_NOT) {
            if (SearchOperation.EMPTY.equals(value)) {
                return builder.isNotEmpty(path.get(fieldName));
            }
            if (SearchOperation.NULL.equals(value)) {
                return builder.isNotNull(path.get(fieldName));
            }
        }
        throw new InvalidOperationException(
                "Unsupported operation " + ops + " " + value + " for collection field " + fieldName
                        + ". Supported: IS EMPTY, IS NOT EMPTY, IS NULL, IS NOT NULL");
    }
}
