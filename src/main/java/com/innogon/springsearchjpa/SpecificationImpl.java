package com.innogon.springsearchjpa;

import com.innogon.springsearchjpa.annotation.SearchSpec;
import com.innogon.springsearchjpa.exception.InvalidFieldException;
import com.innogon.springsearchjpa.exception.InvalidValueException;
import com.innogon.springsearchjpa.strategies.ParsingStrategy;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.jpa.domain.Specification;

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

        if (nestedKey.length > 1
                && searchSpecAnnotation.collectionJoinStrategy() == CollectionJoinStrategy.EXISTS
                && hasCollectionInIntermediatePath(root, nestedKey)) {
            return buildExistsPredicate(root, query, builder, nestedKey);
        }

        From<?, ?> nestedRoot = getNestedRoot(root, nestedKey, query);
        return buildLeafPredicate(builder, nestedRoot, nestedKey);
    }

    private Predicate buildLeafPredicate(CriteriaBuilder builder, From<?, ?> from, String[] nestedKey) {
        String criteriaKey = nestedKey[nestedKey.length - 1];
        boolean isCollectionField = isPluralAttribute(from, criteriaKey);
        Class<?> fieldClass = from.get(criteriaKey).getJavaType();
        ParsingStrategy strategy = ParsingStrategy.getStrategy(fieldClass, searchSpecAnnotation, isCollectionField);
        Object value = parseValue(strategy, fieldClass, criteriaKey, criteria.value());
        return strategy.buildPredicate(builder, from, criteriaKey, criteria.operation(), value);
    }

    // ---- SHARED_JOIN path (default) ----

    private From<?, ?> getNestedRoot(Root<T> root, String[] nestedKey, CriteriaQuery<?> query) {
        if (nestedKey.length <= 1) {
            return root;
        }
        From<?, ?> current = root;
        boolean hasCollectionJoin = false;
        for (int i = 0; i < nestedKey.length - 1; i++) {
            String segment = nestedKey[i];
            boolean isCollection = isPluralAttribute(current, segment);
            JoinType joinType = isCollection ? JoinType.LEFT : JoinType.INNER;
            current = getOrCreateJoin(current, segment, joinType);
            if (isCollection) {
                hasCollectionJoin = true;
            }
        }
        if (hasCollectionJoin) {
            query.distinct(true);
        }
        return current;
    }

    private From<?, ?> getOrCreateJoin(From<?, ?> from, String attribute, JoinType joinType) {
        for (Join<?, ?> join : from.getJoins()) {
            if (join.getAttribute().getName().equals(attribute)) {
                return join;
            }
        }
        return from.join(attribute, joinType);
    }

    // ---- EXISTS path (opt-in) ----

    private Predicate buildExistsPredicate(Root<T> root, CriteriaQuery<?> query,
            CriteriaBuilder builder, String[] nestedKey) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<T> correlatedRoot = subquery.correlate(root);

        From<?, ?> current = correlatedRoot;
        for (int i = 0; i < nestedKey.length - 1; i++) {
            current = current.join(nestedKey[i]);
        }

        Predicate leafPredicate = buildLeafPredicate(builder, current, nestedKey);
        subquery.select(builder.literal(1)).where(leafPredicate);
        return builder.exists(subquery);
    }

    /**
     * Checks whether any intermediate path segment (all except the last) is a
     * collection/plural attribute. Used to decide if the EXISTS strategy applies.
     */
    private boolean hasCollectionInIntermediatePath(Root<T> root, String[] nestedKey) {
        ManagedType<?> currentType = root.getModel();
        for (int i = 0; i < nestedKey.length - 1; i++) {
            try {
                Attribute<?, ?> attr = currentType.getAttribute(nestedKey[i]);
                if (attr instanceof PluralAttribute<?, ?, ?>) {
                    return true;
                }
                if (attr instanceof SingularAttribute<?, ?> sa
                        && sa.getType() instanceof ManagedType<?> mt) {
                    currentType = mt;
                }
            } catch (IllegalArgumentException e) {
                throw new InvalidFieldException(
                        "Field '" + nestedKey[i] + "' does not exist on "
                                + currentType.getJavaType().getSimpleName());
            }
        }
        return false;
    }

    // ---- JPA Metamodel helpers ----

    private boolean isPluralAttribute(From<?, ?> from, String name) {
        ManagedType<?> managedType = getManagedType(from);
        try {
            return managedType.getAttribute(name) instanceof PluralAttribute<?, ?, ?>;
        } catch (IllegalArgumentException e) {
            throw new InvalidFieldException(
                    "Field '" + name + "' does not exist on " + from.getJavaType().getSimpleName());
        }
    }

    private static ManagedType<?> getManagedType(From<?, ?> from) {
        if (from instanceof Root<?> r) return r.getModel();
        if (from instanceof Join<?, ?> j) {
            Attribute<?, ?> attr = j.getAttribute();
            jakarta.persistence.metamodel.Type<?> targetType;
            if (attr instanceof PluralAttribute<?, ?, ?> pa) {
                targetType = pa.getElementType();
            } else if (attr instanceof SingularAttribute<?, ?> sa) {
                targetType = sa.getType();
            } else {
                throw new IllegalStateException("Unexpected attribute type: " + attr.getClass());
            }
            if (targetType instanceof ManagedType<?> mt) return mt;
            throw new IllegalStateException("Join target is not a managed type: " + targetType);
        }
        throw new IllegalStateException("Cannot resolve ManagedType from " + from.getClass());
    }

    // ---- Value parsing ----

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
