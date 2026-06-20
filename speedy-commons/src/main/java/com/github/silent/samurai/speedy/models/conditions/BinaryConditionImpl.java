package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.Literal;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import lombok.Getter;

@Getter
public class BinaryConditionImpl implements BinaryCondition {

    private final QueryField field;
    private final Expression expression;
    private final ConditionOperator operator;

    public BinaryConditionImpl(QueryField field, ConditionOperator operator, Expression expression) throws SpeedyHttpException {
        this.field = field;
        this.operator = operator;
        validateExpression(operator, expression);
        this.expression = expression;
    }

    private static void validateExpression(ConditionOperator operator, Expression expression) throws SpeedyHttpException {
        switch (operator) {
            case IN:
                if (!(expression instanceof Literal)) {
                    throw new BadRequestException("In only accepts a literal");
                }
                break;
            case NOT_IN:
                if (!(expression instanceof Literal)) {
                    throw new BadRequestException("NotIn only accepts a literal");
                }
                break;
            case PATTERN_MATCHING:
                if (!(expression instanceof Literal)) {
                    throw new BadRequestException("Match only accepts a literal");
                }
                break;
            case BETWEEN:
                if (!(expression instanceof Literal)) {
                    throw new BadRequestException("$between only accepts a literal");
                }
                SpeedyValue btValue = ((Literal) expression).value();
                if (!btValue.isCollection()) {
                    throw new BadRequestException("$between only accepts an array");
                }
                if (btValue.asCollection().size() != 2) {
                    throw new BadRequestException("$between requires exactly 2 values");
                }
                break;
            case ISNULL:
                if (!(expression instanceof Literal)) {
                    throw new BadRequestException("$isnull only accepts a literal");
                }
                SpeedyValue isNullValue = ((Literal) expression).value();
                if (!isNullValue.isBoolean()) {
                    throw new BadRequestException("$isnull only accepts a boolean value");
                }
                if (!isNullValue.asBoolean()) {
                    throw new BadRequestException("$isnull requires true. Use $isnotnull for IS NOT NULL");
                }
                break;
            case ISNOTNULL:
                if (!(expression instanceof Literal)) {
                    throw new BadRequestException("$isnotnull only accepts a literal");
                }
                SpeedyValue isNotNullValue = ((Literal) expression).value();
                if (!isNotNullValue.isBoolean()) {
                    throw new BadRequestException("$isnotnull only accepts a boolean value");
                }
                if (!isNotNullValue.asBoolean()) {
                    throw new BadRequestException("$isnotnull requires true. Use $isnull for IS NULL");
                }
                break;
            default:
                break;
        }
    }
}
