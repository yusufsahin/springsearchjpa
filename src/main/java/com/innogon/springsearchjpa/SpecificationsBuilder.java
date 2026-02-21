package com.innogon.springsearchjpa;

import com.innogon.springsearchjpa.annotation.SearchSpec;
import org.springframework.data.jpa.domain.Specification;

public class SpecificationsBuilder<U> {

    private Specification<U> specs = (root, query, cb) -> null;
    private final CriteriaParser<U> parser;

    public SpecificationsBuilder(SearchSpec searchSpecAnnotation) {
        this.parser = new CriteriaParser<>(searchSpecAnnotation);
    }

    /**
     * Parses the given search string and <b>replaces</b> the current specification.
     * Calling this method multiple times overwrites the previous specification.
     * Use {@code AND} / {@code OR} operators inside the search string itself to combine criteria.
     *
     * @param search the search query string (e.g. {@code "name:John AND age>25"})
     * @return this builder for method chaining
     */
    public SpecificationsBuilder<U> withSearch(String search) {
        specs = parser.parse(search);
        return this;
    }

    /**
     * Builds and returns the JPA {@link Specification} from the parsed search string.
     * If no search string was provided, returns a no-op specification.
     *
     * @return a {@link Specification} matching the parsed search criteria
     */
    public Specification<U> build() {
        return specs;
    }
}
