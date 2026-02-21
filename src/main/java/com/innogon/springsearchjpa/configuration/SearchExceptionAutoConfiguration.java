package com.innogon.springsearchjpa.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Auto-configures {@link SearchExceptionHandler} to convert
 * {@code SearchQueryException} from {@code toPredicate()} into HTTP 400.
 * <p>
 * Opt out by setting {@code spring-search.exception-handler.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnProperty(
        name = "spring-search.exception-handler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Import(SearchExceptionHandler.class)
public class SearchExceptionAutoConfiguration {
}
