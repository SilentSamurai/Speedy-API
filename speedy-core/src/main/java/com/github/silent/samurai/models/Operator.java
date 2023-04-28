package com.github.silent.samurai.models;

import com.github.silent.samurai.exceptions.BadRequestException;

public enum Operator {
    EQ, NEQ, LT, GT, LTE, GTE, IN, NOT_IN;

    public static Operator fromSymbol(String symbol) throws BadRequestException {
        if (symbol.equals("=")) {
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
        } else if (symbol.equals("in")) {
            return IN;
        } else if (symbol.equals("ine")) {
            return NOT_IN;
        } else {
            throw new BadRequestException("");
        }
    }
}
