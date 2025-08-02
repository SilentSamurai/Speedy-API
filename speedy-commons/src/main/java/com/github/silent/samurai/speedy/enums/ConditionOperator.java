package com.github.silent.samurai.speedy.enums;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;

public enum ConditionOperator {
    EQ, NEQ, LT, GT, LTE, GTE, IN, NOT_IN, AND, OR, PATTERN_MATCHING;

    public static ConditionOperator fromSymbol(String symbol) throws BadRequestException {
        return switch (symbol) {
            case "=", "==", "$eq" -> EQ;
            case "!=", "$neq", "$ne" -> NEQ;
            case "<", "$lt" -> LT;
            case "<=", "$lte" -> LTE;
            case ">", "$gt" -> GT;
            case ">=", "$gte" -> GTE;
            case "=*", "$matches" -> PATTERN_MATCHING;
            case "<>", "$in" -> IN;
            case "<!>", "$nin" -> NOT_IN;
            case "&", ",", "&&", "$and" -> AND;
            case "|", "||", "$or" -> OR;
            default -> throw new BadRequestException("Operator not recognized: " + symbol);
        };
    }

    public boolean doesAcceptMultipleValues() {
        return this == IN || this == NOT_IN;
    }
}
