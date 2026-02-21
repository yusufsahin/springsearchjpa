package com.innogon.springsearchjpa.strategies;

import com.innogon.springsearchjpa.SearchOperation;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class DateStrategy implements ParsingStrategy {

    private static final List<String> DATE_FORMATS = List.of(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
    );

    @Override
    public Predicate buildPredicate(
            CriteriaBuilder builder,
            Path<?> path,
            String fieldName,
            SearchOperation ops,
            Object value) {
        if (value instanceof Date dateValue) {
            Predicate comparison = buildComparisonPredicate(builder, path.get(fieldName), ops, dateValue);
            if (comparison != null) return comparison;
        }
        return ParsingStrategy.super.buildPredicate(builder, path, fieldName, ops, value);
    }

    @Override
    public Object parse(String value, Class<?> fieldClass) {
        if (SearchOperation.NULL.equals(value)) return value;
        for (String format : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                sdf.setLenient(false);
                return sdf.parse(value);
            } catch (ParseException ignored) {
            }
        }
        throw new IllegalArgumentException("Failed to parse date: " + value
                + ". Supported formats: yyyy-MM-dd, yyyy-MM-dd'T'HH:mm:ss, etc.");
    }
}
