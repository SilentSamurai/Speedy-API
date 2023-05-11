package com.github.silent.samurai.models;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;

public enum Operator {
    EQ, NEQ, LT, GT, LTE, GTE, IN, NOT_IN, AND, OR;

    public boolean doesAcceptMultipleValues() {
        return this == IN || this == NOT_IN;
    }

    public static Operator fromSymbol(String symbol) throws BadRequestException {
        if (symbol.equals("=") || symbol.equals("==")) {
            return EQ;
        } else if (symbol.equals("!=")) {
            return NEQ;
        } else if (symbol.equals("<")) {
            return LT;
        } else if (symbol.equals("<=")) {
            return LTE;
        } else if (symbol.equals(">")) {
            return GT;
        } else if (symbol.equals(">=")) {
            return GTE;
        } else if (symbol.equals("<>")) {
            return IN;
        } else if (symbol.equals("<!>")) {
            return NOT_IN;
        } else if (symbol.equals("&") || symbol.equals(",")) {
            return AND;
        } else if (symbol.equals("|")) {
            return OR;
        } else {
            throw new BadRequestException("");
        }
    }
}
