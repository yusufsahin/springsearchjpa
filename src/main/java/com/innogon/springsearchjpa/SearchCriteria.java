package com.innogon.springsearchjpa;

/**
 * Immutable representation of a search criterion (field + operation + value).
 */
public record SearchCriteria(String key, SearchOperation operation, Object value) {

    public SearchCriteria(String key, SearchOperation op, String prefix, Object value, String suffix) {
        this(key, resolveOperation(op, prefix, suffix), value);
    }

    private static SearchOperation resolveOperation(SearchOperation op, String prefix, String suffix) {
        boolean startsWithAsterisk = prefix != null && prefix.contains(SearchOperation.ZERO_OR_MORE_REGEX);
        boolean endsWithAsterisk = suffix != null && suffix.contains(SearchOperation.ZERO_OR_MORE_REGEX);

        if (op == SearchOperation.EQUALS) {
            if (startsWithAsterisk && endsWithAsterisk) return SearchOperation.CONTAINS;
            if (startsWithAsterisk) return SearchOperation.ENDS_WITH;
            if (endsWithAsterisk) return SearchOperation.STARTS_WITH;
        }

        if (op == SearchOperation.NOT_EQUALS) {
            if (startsWithAsterisk && endsWithAsterisk) return SearchOperation.DOESNT_CONTAIN;
            if (startsWithAsterisk) return SearchOperation.DOESNT_END_WITH;
            if (endsWithAsterisk) return SearchOperation.DOESNT_START_WITH;
        }

        return op;
    }
}
