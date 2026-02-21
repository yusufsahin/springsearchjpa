package com.innogon.springsearchjpa.strategies;

import com.innogon.springsearchjpa.SearchOperation;
import com.innogon.springsearchjpa.annotation.SearchSpec;
import com.innogon.springsearchjpa.exception.InvalidOperationException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface ParsingStrategy {

    char LIKE_ESCAPE_CHAR = '\\';

    /**
     * Method to parse the value specified to the corresponding strategy.
     *
     * @param value      Value used for the search
     * @param fieldClass Java class of the referred field
     * @return Returns by default the value without any parsing
     */
    default Object parse(String value, Class<?> fieldClass) {
        return value;
    }

    default Object parse(List<?> value, Class<?> fieldClass) {
        return value == null ? null : value.stream()
                .map(v -> parse(v.toString(), fieldClass))
                .toList();
    }

    /**
     * Escapes SQL LIKE wildcards ({@code %} and {@code _}) in user input
     * to prevent wildcard injection.
     */
    static String escapeLikeValue(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * Builds a comparison predicate for Comparable types.
     * Eliminates repetitive GREATER_THAN/LESS_THAN logic across strategies.
     */
    default <Y extends Comparable<Y>> Predicate buildComparisonPredicate(
            CriteriaBuilder builder,
            Path<? extends Y> field,
            SearchOperation op,
            Y value) {
        return switch (op) {
            case GREATER_THAN -> builder.greaterThan(field, value);
            case LESS_THAN -> builder.lessThan(field, value);
            case GREATER_THAN_EQUALS -> builder.greaterThanOrEqualTo(field, value);
            case LESS_THAN_EQUALS -> builder.lessThanOrEqualTo(field, value);
            default -> null;
        };
    }

    /**
     * Method to build the predicate.
     *
     * @param builder   Criteria object to build on
     * @param path      Current path for predicate
     * @param fieldName Name of the field to be searched
     * @param ops       Search operation to use
     * @param value     Value used for the search
     * @return Returns a Predicate instance
     */
    default Predicate buildPredicate(
            CriteriaBuilder builder,
            Path<?> path,
            String fieldName,
            SearchOperation ops,
            Object value) {
        if (value == null && ops != SearchOperation.IS && ops != SearchOperation.IS_NOT) {
            throw new InvalidOperationException(
                    "Null value is only supported with IS / IS NOT operations for field " + fieldName);
        }
        return switch (ops) {
            case IN_ARRAY -> {
                CriteriaBuilder.In<Object> inClause = getInClause(builder, path, fieldName, value);
                yield inClause;
            }
            case NOT_IN_ARRAY -> {
                CriteriaBuilder.In<Object> inClause = getInClause(builder, path, fieldName, value);
                yield builder.not(inClause);
            }
            case IS -> {
                if (SearchOperation.NULL.equals(value)) {
                    yield builder.isNull(path.get(fieldName));
                }
                throw new InvalidOperationException(
                        "Unsupported operation " + ops + " " + value + " for field " + fieldName);
            }
            case IS_NOT -> {
                if (SearchOperation.NULL.equals(value)) {
                    yield builder.isNotNull(path.get(fieldName));
                }
                throw new InvalidOperationException(
                        "Unsupported operation " + ops + " " + value + " for field " + fieldName);
            }
            case EQUALS -> builder.equal(path.get(fieldName), value);
            case NOT_EQUALS -> builder.notEqual(path.get(fieldName), value);
            case STARTS_WITH -> builder.like(path.get(fieldName).as(String.class),
                    escapeLikeValue(value.toString()) + "%", LIKE_ESCAPE_CHAR);
            case ENDS_WITH -> builder.like(path.get(fieldName).as(String.class),
                    "%" + escapeLikeValue(value.toString()), LIKE_ESCAPE_CHAR);
            case CONTAINS -> builder.like(path.get(fieldName).as(String.class),
                    "%" + escapeLikeValue(value.toString()) + "%", LIKE_ESCAPE_CHAR);
            case DOESNT_START_WITH -> builder.notLike(path.get(fieldName).as(String.class),
                    escapeLikeValue(value.toString()) + "%", LIKE_ESCAPE_CHAR);
            case DOESNT_END_WITH -> builder.notLike(path.get(fieldName).as(String.class),
                    "%" + escapeLikeValue(value.toString()), LIKE_ESCAPE_CHAR);
            case DOESNT_CONTAIN -> builder.notLike(path.get(fieldName).as(String.class),
                    "%" + escapeLikeValue(value.toString()) + "%", LIKE_ESCAPE_CHAR);
            default -> throw new InvalidOperationException(
                    "Unsupported operation: " + ops + " for field " + fieldName);
        };
    }

    default CriteriaBuilder.In<Object> getInClause(
            CriteriaBuilder builder,
            Path<?> path,
            String fieldName,
            Object value) {
        List<?> values = (List<?>) value;
        if (values.isEmpty()) {
            throw new InvalidOperationException(
                    "IN / NOT IN requires at least one value for field " + fieldName);
        }
        CriteriaBuilder.In<Object> inClause = builder.in(path.get(fieldName));
        for (Object v : values) {
            inClause.value(v);
        }
        return inClause;
    }

    static ParsingStrategy getStrategy(Class<?> fieldClass, SearchSpec searchSpecAnnotation, boolean isCollectionField) {
        if (isCollectionField) return new CollectionStrategy();
        if (fieldClass == Boolean.class || fieldClass == boolean.class) return new BooleanStrategy();
        if (Date.class.isAssignableFrom(fieldClass)) return new DateStrategy();
        if (fieldClass == Long.class || fieldClass == long.class) return new ComparableStrategy<>(Long.class, Long::parseLong);
        if (fieldClass == Double.class || fieldClass == double.class) return new ComparableStrategy<>(Double.class, Double::parseDouble);
        if (fieldClass == Float.class || fieldClass == float.class) return new ComparableStrategy<>(Float.class, Float::parseFloat);
        if (fieldClass == Integer.class || fieldClass == int.class) return new ComparableStrategy<>(Integer.class, Integer::parseInt);
        if (fieldClass == Short.class || fieldClass == short.class) return new ComparableStrategy<>(Short.class, Short::parseShort);
        if (fieldClass == Byte.class || fieldClass == byte.class) return new ComparableStrategy<>(Byte.class, Byte::parseByte);
        if (fieldClass == BigDecimal.class) return new ComparableStrategy<>(BigDecimal.class, BigDecimal::new);
        if (Enum.class.isAssignableFrom(fieldClass)) return new EnumStrategy();
        if (fieldClass == Duration.class) return new ComparableStrategy<>(Duration.class, Duration::parse);
        if (fieldClass == LocalDate.class) return new ComparableStrategy<>(LocalDate.class, LocalDate::parse);
        if (fieldClass == LocalTime.class) return new ComparableStrategy<>(LocalTime.class, LocalTime::parse);
        if (fieldClass == LocalDateTime.class) return new ComparableStrategy<>(LocalDateTime.class, LocalDateTime::parse);
        if (fieldClass == Instant.class) return new ComparableStrategy<>(Instant.class, Instant::parse);
        if (fieldClass == UUID.class) return new UUIDStrategy();
        return new StringStrategy(searchSpecAnnotation);
    }
}
