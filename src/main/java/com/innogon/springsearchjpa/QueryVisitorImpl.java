package com.innogon.springsearchjpa;

import com.innogon.springsearchjpa.annotation.SearchSpec;
import com.innogon.springsearchjpa.exception.InvalidFieldException;
import com.innogon.springsearchjpa.exception.InvalidOperationException;
import com.innogon.springsearchjpa.grammar.QueryBaseVisitor;
import com.innogon.springsearchjpa.grammar.QueryParser;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryVisitorImpl<T> extends QueryBaseVisitor<Specification<T>> {

    private static final Pattern VALUE_PATTERN = Pattern.compile("^(?<prefix>\\*?)(?<value>.+?)(?<suffix>\\*?)$");

    private final SearchSpec searchSpecAnnotation;
    private final Set<String> whiteListSet;
    private final Set<String> blackListSet;

    public QueryVisitorImpl(SearchSpec searchSpecAnnotation) {
        this.searchSpecAnnotation = searchSpecAnnotation;
        this.whiteListSet = Set.of(searchSpecAnnotation.whiteListedFields());
        this.blackListSet = Set.of(searchSpecAnnotation.blackListedFields());
    }

    @Override
    public Specification<T> visitOpQuery(QueryParser.OpQueryContext ctx) {
        Specification<T> left = visit(ctx.left);
        Specification<T> right = visit(ctx.right);

        return switch (ctx.logicalOp.getText()) {
            case "AND" -> left.and(right);
            case "OR" -> left.or(right);
            default -> left.and(right);
        };
    }

    @Override
    public Specification<T> visitPriorityQuery(QueryParser.PriorityQueryContext ctx) {
        return visit(ctx.query());
    }

    @Override
    public Specification<T> visitAtomQuery(QueryParser.AtomQueryContext ctx) {
        return visit(ctx.criteria());
    }

    @Override
    public Specification<T> visitInput(QueryParser.InputContext ctx) {
        return visit(ctx.query());
    }

    @Override
    public Specification<T> visitIsCriteria(QueryParser.IsCriteriaContext ctx) {
        String key = ctx.key().getText();
        verifyFieldAccess(key);
        SearchOperation op = ctx.IS() != null ? SearchOperation.IS : SearchOperation.IS_NOT;
        return toSpec(key, op, ctx.is_value().getText());
    }

    @Override
    public Specification<T> visitEqArrayCriteria(QueryParser.EqArrayCriteriaContext ctx) {
        String key = ctx.key().getText();
        verifyFieldAccess(key);
        SearchOperation op = ctx.IN() != null ? SearchOperation.IN_ARRAY : SearchOperation.NOT_IN_ARRAY;
        List<String> valueAsList = ctx.array().value().stream()
                .map(v -> v.STRING() != null ? clearString(v.getText()) : v.getText())
                .toList();
        SearchCriteria criteria = new SearchCriteria(key, op, valueAsList);
        return new SpecificationImpl<>(criteria, searchSpecAnnotation);
    }

    @Override
    public Specification<T> visitBetweenCriteria(QueryParser.BetweenCriteriaContext ctx) {
        String key = ctx.key().getText();
        verifyFieldAccess(key);
        String leftValue = ctx.left.STRING() != null ? clearString(ctx.left.getText()) : ctx.left.getText();
        String rightValue = ctx.right.STRING() != null ? clearString(ctx.right.getText()) : ctx.right.getText();
        Specification<T> leftExp = toSpec(key, SearchOperation.GREATER_THAN_EQUALS, leftValue);
        Specification<T> rightExp = toSpec(key, SearchOperation.LESS_THAN_EQUALS, rightValue);
        if (ctx.BETWEEN() != null) {
            return leftExp.and(rightExp);
        } else {
            return Specification.not(leftExp.and(rightExp));
        }
    }

    @Override
    public Specification<T> visitOpCriteria(QueryParser.OpCriteriaContext ctx) {
        String key = ctx.key().getText();
        String value = ctx.value().STRING() != null ? clearString(ctx.value().getText()) : ctx.value().getText();
        verifyFieldAccess(key);
        Matcher matchResult = VALUE_PATTERN.matcher(value);
        SearchOperation op = SearchOperation.getSimpleOperation(ctx.op().getText());
        if (op == null) {
            throw new InvalidOperationException("Invalid operation: " + ctx.op().getText());
        }
        if (!matchResult.matches()) {
            throw new InvalidOperationException("Invalid value: " + value);
        }
        SearchCriteria criteria = new SearchCriteria(
                key,
                op,
                matchResult.group("prefix"),
                matchResult.group("value"),
                matchResult.group("suffix")
        );
        return new SpecificationImpl<>(criteria, searchSpecAnnotation);
    }

    private SpecificationImpl<T> toSpec(String key, SearchOperation op, String value) {
        SearchCriteria criteria = new SearchCriteria(key, op, value);
        return new SpecificationImpl<>(criteria, searchSpecAnnotation);
    }

    private void verifyFieldAccess(String key) {
        if (!whiteListSet.isEmpty()) {
            if (!whiteListSet.contains(key)) {
                String root = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
                if (!whiteListSet.contains(root)) {
                    throw new InvalidFieldException("Field " + key + " is not allowed");
                }
            }
            return;
        }

        if (!blackListSet.isEmpty()) {
            String[] parts = key.split("\\.");
            for (String part : parts) {
                if (blackListSet.contains(part)) {
                    throw new InvalidFieldException("Field " + key + " is blacklisted");
                }
            }
        }
    }

    private String clearString(String value) {
        if (value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
        } else if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return unescapeString(value);
    }

    private String unescapeString(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                switch (next) {
                    case '"' -> { sb.append('"'); i++; }
                    case '\'' -> { sb.append('\''); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case 'b' -> { sb.append('\b'); i++; }
                    case 'f' -> { sb.append('\f'); i++; }
                    case 'n' -> { sb.append('\n'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
