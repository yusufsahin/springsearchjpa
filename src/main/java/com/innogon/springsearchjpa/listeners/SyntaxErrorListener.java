package com.innogon.springsearchjpa.listeners;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

public class SyntaxErrorListener extends BaseErrorListener {

    private final List<String> messages = new ArrayList<>();

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e) {
        messages.add("line " + line + ":" + charPositionInLine + " " + msg);
    }

    public boolean hasErrors() {
        return !messages.isEmpty();
    }

    @Override
    public String toString() {
        return messages.toString();
    }
}
