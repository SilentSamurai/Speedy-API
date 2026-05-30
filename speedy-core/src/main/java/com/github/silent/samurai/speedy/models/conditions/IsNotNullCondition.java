package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.Literal;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.models.SpeedyBoolean;
import lombok.Getter;

/**
 * Condition representing {@code $isnotnull} — an IS NOT NULL check.
 * <p>
 * Expects a literal boolean {@code true}.
 * Shorthand form ({@code "field": "$isnotnull"}) is intercepted in the JSON parser.
 */
@Getter
public class IsNotNullCondition implements BinaryCondition {

    private final QueryField field;
    private final ConditionOperator operator = ConditionOperator.ISNOTNULL;

    /** Must be a boolean literal with value {@code true}. */
    private final Literal expression;

    /**
     * @param field      the left-hand field
     * @param expression must be a {@link Literal} wrapping a {@link SpeedyBoolean} with value {@code true}
     * @throws BadRequestException if the value is not a boolean, or is {@code false}
     */
    public IsNotNullCondition(QueryField field, Expression expression) throws SpeedyHttpException {
        this.field = field;
        if (!(expression instanceof Literal)) {
            throw new BadRequestException("$isnotnull only accepts a literal");
        }
        SpeedyValue value = ((Literal) expression).value();
        if (!value.isBoolean()) {
            throw new BadRequestException("$isnotnull only accepts a boolean value");
        }
        if (!value.asBoolean()) {
            throw new BadRequestException("$isnotnull requires true. Use $isnull for IS NULL");
        }
        this.expression = (Literal) expression;
    }
}
