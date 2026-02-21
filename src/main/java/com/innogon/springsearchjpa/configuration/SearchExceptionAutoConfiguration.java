package com.innogon.springsearchjpa.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configures {@link SearchExceptionHandler} to convert
 * {@code SearchQueryException} from {@code toPredicate()} into HTTP 400.
 * <p>
 * Opt out by setting {@code spring-search.exception-handler.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
        name = "spring-search.exception-handler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SearchExceptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SearchExceptionHandler.class)
    public SearchExceptionHandler searchExceptionHandler() {
        return new SearchExceptionHandler();
    }
}
