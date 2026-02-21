package com.innogon.springsearchjpa;

import com.innogon.springsearchjpa.annotation.SearchSpec;
import com.innogon.springsearchjpa.exception.SearchQueryException;
import com.innogon.springsearchjpa.grammar.QueryLexer;
import com.innogon.springsearchjpa.grammar.QueryParser;
import com.innogon.springsearchjpa.listeners.SyntaxErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.data.jpa.domain.Specification;

/**
 * Class used to parse a search query string and create a specification.
 */
public class CriteriaParser<T> {

    private final QueryVisitorImpl<T> visitor;

    public CriteriaParser(SearchSpec searchSpecAnnotation) {
        this.visitor = new QueryVisitorImpl<>(searchSpecAnnotation);
    }

    /**
     * Lexer -> Parser -> Visitor are used to build the specification.
     *
     * @param searchParam The search param
     * @return a specification matching the input
     */
    public Specification<T> parse(String searchParam) {
        SyntaxErrorListener listener = new SyntaxErrorListener();
        QueryLexer lexer = new QueryLexer(CharStreams.fromString(searchParam));
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        QueryParser parser = new QueryParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);

        QueryParser.InputContext input = parser.input();
        if (listener.hasErrors()) {
            throw new SearchQueryException("Invalid search query: " + listener);
        }
        return visitor.visit(input);
    }
}
