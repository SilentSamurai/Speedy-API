package com.github.silent.samurai.speedy.enums;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;

public enum ConditionOperator {
    EQ, NEQ, LT, GT, LTE, GTE, IN, NOT_IN, AND, OR, PATTERN_MATCHING;

    public static ConditionOperator fromSymbol(String symbol) throws BadRequestException {
        if (symbol.equals("=") || symbol.equals("==") || symbol.equals("$eq")) {
            return EQ;
        } else if (symbol.equals("!=") || symbol.equals("$neq") || symbol.equals("$ne")) {
            return NEQ;
        } else if (symbol.equals("<") || symbol.equals("$lt")) {
            return LT;
        } else if (symbol.equals("<=") || symbol.equals("$lte")) {
            return LTE;
        } else if (symbol.equals(">") || symbol.equals("$gt")) {
            return GT;
        } else if (symbol.equals(">=") || symbol.equals("$gte")) {
            return GTE;
        } else if (symbol.equals("=*") || symbol.equals("$matches")) {
            return PATTERN_MATCHING;
        } else if (symbol.equals("<>") || symbol.equals("$in")) {
            return IN;
        } else if (symbol.equals("<!>") || symbol.equals("$nin")) {
            return NOT_IN;
        } else if (symbol.equals("&") || symbol.equals(",") || symbol.equals("&&") || symbol.equals("$and")) {
            return AND;
        } else if (symbol.equals("|") || symbol.equals("||") || symbol.equals("$or")) {
            return OR;
        } else {
            throw new BadRequestException("Operator not recognized: " + symbol);
        }
    }

    public boolean doesAcceptMultipleValues() {
        return this == IN || this == NOT_IN;
    }
}
