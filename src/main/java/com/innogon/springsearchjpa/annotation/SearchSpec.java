package com.innogon.springsearchjpa.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for mapping query search string into a Specification for a JPA Domain.
 *
 * This annotation can be used on a parameter in a Spring MVC RestRepository Class.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface SearchSpec {

    /**
     * The name of the query param that will be transformed into a specification.
     */
    String searchParam() default "search";

    /**
     * A flag to indicate if the search needs to be case-sensitive or not.
     */
    boolean caseSensitiveFlag() default true;

    /**
     * A list of fields that should be excluded from the search.
     */
    String[] blackListedFields() default {};

    /**
     * A list of fields that are allowed in the search.
     * When non-empty, only these fields can be searched (whitelist takes precedence over blacklist).
     */
    String[] whiteListedFields() default {};

    /**
     * Whether the search parameter is required. When true, a missing or empty search
     * parameter will result in a 400 Bad Request.
     */
    boolean required() default false;

    /**
     * Default search query to use when the search parameter is empty or missing.
     * Ignored when {@link #required()} is true.
     */
    String defaultValue() default "";

    /**
     * Maximum allowed length for the search query string. Queries exceeding this
     * length will result in a 400 Bad Request.
     */
    int maxLength() default 1024;
}
