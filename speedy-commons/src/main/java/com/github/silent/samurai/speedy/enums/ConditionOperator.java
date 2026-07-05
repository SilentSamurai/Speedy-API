package com.github.silent.samurai.speedy.enums;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;

public enum ConditionOperator {
    EQ, NEQ, LT, GT, LTE, GTE, IN, NOT_IN, AND, OR, PATTERN_MATCHING, BETWEEN, ISNULL, ISNOTNULL;

    public static ConditionOperator fromSymbol(String symbol) throws BadRequestException {
        return switch (symbol) {
            case "=", "==", "$eq", "eq" -> EQ;
            case "!=", "$neq", "$ne", "neq", "ne" -> NEQ;
            case "<", "$lt", "lt" -> LT;
            case "<=", "$lte", "lte" -> LTE;
            case ">", "$gt", "gt" -> GT;
            case ">=", "$gte", "gte" -> GTE;
            case "=*", "$matches", "matches" -> PATTERN_MATCHING;
            case "<>", "$in", "in" -> IN;
            case "<!>", "$nin", "nin" -> NOT_IN;
            case "&", ",", "&&", "$and", "and" -> AND;
            case "$between", "between" -> BETWEEN;
            case "$isnull", "isnull" -> ISNULL;
            case "$isnotnull", "isnotnull" -> ISNOTNULL;
            case "|", "||", "$or", "or" -> OR;
            default -> throw new BadRequestException("Operator not recognized: " + symbol);
        };
    }

    public boolean doesAcceptMultipleValues() {
        return this == IN || this == NOT_IN || this == BETWEEN;
    }
}
