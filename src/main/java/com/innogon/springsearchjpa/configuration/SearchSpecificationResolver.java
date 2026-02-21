package com.innogon.springsearchjpa.configuration;

import com.innogon.springsearchjpa.SpecificationsBuilder;
import com.innogon.springsearchjpa.annotation.SearchSpec;
import com.innogon.springsearchjpa.exception.SearchQueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class SearchSpecificationResolver implements HandlerMethodArgumentResolver {

    private static final Logger logger = LoggerFactory.getLogger(SearchSpecificationResolver.class);

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.getParameterType() == Specification.class
                && parameter.hasParameterAnnotation(SearchSpec.class);
    }

    @Override
    public Specification<?> resolveArgument(
            @NonNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) throws Exception {

        SearchSpec def = parameter.getParameterAnnotation(SearchSpec.class);
        if (def == null) {
            return null;
        }

        Class<?> entityClass = resolveEntityClass(parameter);
        String search = webRequest.getParameter(def.searchParam());

        return buildSpecification(entityClass, search, def);
    }

    /**
     * Resolves the actual entity class from the generic Specification type parameter.
     * For example, Specification&lt;Users&gt; resolves to Users.class.
     */
    private Class<?> resolveEntityClass(MethodParameter parameter) {
        Type genericType = parameter.getGenericParameterType();
        if (genericType instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> clazz) {
                return clazz;
            }
        }
        return Object.class;
    }

    private <T> Specification<T> buildSpecification(Class<T> specClass, String search, SearchSpec annotation) {
        logger.debug("Building specification for class {}", specClass);
        logger.debug("Search value found is {}", search);

        if (search == null || search.isBlank()) {
            if (annotation.required()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Search parameter '" + annotation.searchParam() + "' is required");
            }

            if (!annotation.defaultValue().isEmpty()) {
                search = annotation.defaultValue();
            } else {
                return (root, query, cb) -> null;
            }
        }

        if (search.length() > annotation.maxLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Search query too long (max " + annotation.maxLength() + " characters)");
        }

        try {
            SpecificationsBuilder<T> specBuilder = new SpecificationsBuilder<>(annotation);
            return specBuilder.withSearch(search).build();
        } catch (SearchQueryException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
