package com.innogon.springsearchjpa.strategies;

import com.innogon.springsearchjpa.SearchOperation;
import com.innogon.springsearchjpa.annotation.SearchSpec;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.List;
import java.util.Locale;

public class StringStrategy implements ParsingStrategy {

    private final SearchSpec searchSpecAnnotation;

    public StringStrategy(SearchSpec searchSpecAnnotation) {
        this.searchSpecAnnotation = searchSpecAnnotation;
    }

    @Override
    public Predicate buildPredicate(
            CriteriaBuilder builder,
            Path<?> path,
            String fieldName,
            SearchOperation ops,
            Object value) {
        if (value instanceof List<?>) {
            return ParsingStrategy.super.buildPredicate(builder, path, fieldName, ops, value);
        }

        String casedValue = searchSpecAnnotation.caseSensitiveFlag()
                ? value.toString()
                : value.toString().toLowerCase(Locale.ROOT);

        Expression<String> casedField = searchSpecAnnotation.caseSensitiveFlag()
                ? path.get(fieldName)
                : builder.lower(path.get(fieldName));

        return switch (ops) {
            case EQUALS -> builder.equal(casedField, casedValue);
            case NOT_EQUALS -> builder.notEqual(casedField, casedValue);
            case STARTS_WITH, ENDS_WITH, CONTAINS,
                 DOESNT_START_WITH, DOESNT_END_WITH, DOESNT_CONTAIN -> {
                String escaped = ParsingStrategy.escapeLikeValue(casedValue);
                yield buildLikePredicate(builder, casedField, ops, escaped);
            }
            case GREATER_THAN -> builder.greaterThan(casedField, casedValue);
            case GREATER_THAN_EQUALS -> builder.greaterThanOrEqualTo(casedField, casedValue);
            case LESS_THAN -> builder.lessThan(casedField, casedValue);
            case LESS_THAN_EQUALS -> builder.lessThanOrEqualTo(casedField, casedValue);
            default -> ParsingStrategy.super.buildPredicate(builder, path, fieldName, ops, value);
        };
    }

    private Predicate buildLikePredicate(
            CriteriaBuilder builder,
            Expression<String> field,
            SearchOperation ops,
            String escaped) {
        return switch (ops) {
            case STARTS_WITH -> builder.like(field, escaped + "%", LIKE_ESCAPE_CHAR);
            case ENDS_WITH -> builder.like(field, "%" + escaped, LIKE_ESCAPE_CHAR);
            case CONTAINS -> builder.like(field, "%" + escaped + "%", LIKE_ESCAPE_CHAR);
            case DOESNT_START_WITH -> builder.notLike(field, escaped + "%", LIKE_ESCAPE_CHAR);
            case DOESNT_END_WITH -> builder.notLike(field, "%" + escaped, LIKE_ESCAPE_CHAR);
            case DOESNT_CONTAIN -> builder.notLike(field, "%" + escaped + "%", LIKE_ESCAPE_CHAR);
            default -> throw new IllegalStateException("Unexpected LIKE operation: " + ops);
        };
    }
}
