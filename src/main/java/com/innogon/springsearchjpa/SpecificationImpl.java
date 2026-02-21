package com.innogon.springsearchjpa;

import com.innogon.springsearchjpa.annotation.SearchSpec;
import com.innogon.springsearchjpa.exception.InvalidValueException;
import com.innogon.springsearchjpa.strategies.ParsingStrategy;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;

/**
 * Implementation of the JPA Specification based on a Search Criteria.
 *
 * @param <T> The class on which the specification will be applied
 * @see Specification
 */
public class SpecificationImpl<T> implements Specification<T> {

    private final SearchCriteria criteria;
    private final SearchSpec searchSpecAnnotation;

    public SpecificationImpl(SearchCriteria criteria, SearchSpec searchSpecAnnotation) {
        this.criteria = criteria;
        this.searchSpecAnnotation = searchSpecAnnotation;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        String[] nestedKey = criteria.key().split("\\.");
        Path<?> nestedRoot = getNestedRoot(root, nestedKey);
        String criteriaKey = nestedKey[nestedKey.length - 1];
        Class<?> fieldClass = nestedRoot.get(criteriaKey).getJavaType();
        boolean isCollectionField = isCollectionType(nestedRoot.getJavaType(), criteriaKey);
        ParsingStrategy strategy = ParsingStrategy.getStrategy(fieldClass, searchSpecAnnotation, isCollectionField);
        Object value = parseValue(strategy, fieldClass, criteriaKey, criteria.value());
        return strategy.buildPredicate(builder, nestedRoot, criteriaKey, criteria.operation(), value);
    }

    private Path<?> getNestedRoot(Root<T> root, String[] nestedKey) {
        Path<?> temp = root;
        for (int i = 0; i < nestedKey.length - 1; i++) {
            temp = temp.get(nestedKey[i]);
        }
        return temp;
    }

    private boolean isCollectionType(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(fieldName);
                Class<?> type = field.getType();
                return Collection.class.isAssignableFrom(type) || type.isArray();
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return false;
    }

    private Object parseValue(ParsingStrategy strategy, Class<?> fieldClass, String criteriaKey, Object value) {
        try {
            if (value instanceof List<?> listValue) {
                return strategy.parse(listValue, fieldClass);
            } else {
                return strategy.parse(value != null ? value.toString() : null, fieldClass);
            }
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            throw new InvalidValueException(
                    "Could not parse input for the field " + criteriaKey + " as a " + fieldClass.getSimpleName(), e);
        }
    }
}
