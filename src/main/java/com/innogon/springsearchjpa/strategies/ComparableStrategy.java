package com.innogon.springsearchjpa.strategies;

import com.innogon.springsearchjpa.SearchOperation;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.function.Function;

/**
 * Generic strategy for all {@link Comparable} field types.
 * Eliminates the need for separate IntStrategy, FloatStrategy, etc.
 *
 * @param <T> the type this strategy handles (must be Comparable at runtime)
 */
public class ComparableStrategy<T> implements ParsingStrategy {

    private final Class<T> type;
    private final Function<String, T> parser;

    public ComparableStrategy(Class<T> type, Function<String, T> parser) {
        this.type = type;
        this.parser = parser;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Predicate buildPredicate(
            CriteriaBuilder builder,
            Path<?> path,
            String fieldName,
            SearchOperation ops,
            Object value) {
        if (type.isInstance(value) && value instanceof Comparable) {
            Path field = path.get(fieldName);
            Comparable comp = (Comparable) value;
            return switch (ops) {
                case GREATER_THAN -> builder.greaterThan(field, comp);
                case LESS_THAN -> builder.lessThan(field, comp);
                case GREATER_THAN_EQUALS -> builder.greaterThanOrEqualTo(field, comp);
                case LESS_THAN_EQUALS -> builder.lessThanOrEqualTo(field, comp);
                default -> ParsingStrategy.super.buildPredicate(builder, path, fieldName, ops, value);
            };
        }
        return ParsingStrategy.super.buildPredicate(builder, path, fieldName, ops, value);
    }

    @Override
    public Object parse(String value, Class<?> fieldClass) {
        if (SearchOperation.NULL.equals(value)) return value;
        return parser.apply(value);
    }
}
