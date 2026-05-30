package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.Literal;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import lombok.Getter;

/**
 * Condition representing {@code $between} — an inclusive range check.
 * <p>
 * Expects a literal array of exactly 2 values (low and high bound).
 * Equivalent to {@code field &gt;= low AND field &lt;= high}.
 */
@Getter
public class BetweenCondition implements BinaryCondition {

    private final QueryField field;
    private final ConditionOperator operator = ConditionOperator.BETWEEN;

    /** The array literal containing exactly [low, high] bounds. */
    private final Literal expression;

    /**
     * @param field      the left-hand field
     * @param expression must be a {@link Literal} wrapping a {@link SpeedyCollection} of exactly 2 values
     * @throws BadRequestException if the expression is not a literal, not a collection, or does not contain exactly 2 values
     */
    public BetweenCondition(QueryField field, Expression expression) throws SpeedyHttpException {
        this.field = field;
        if (!(expression instanceof Literal)) {
            throw new BadRequestException("$between only accepts a literal");
        }
        SpeedyValue value = ((Literal) expression).value();
        if (!(value instanceof SpeedyCollection)) {
            throw new BadRequestException("$between only accepts an array");
        }
        SpeedyCollection collection = (SpeedyCollection) value;
        if (collection.getValue().size() != 2) {
            throw new BadRequestException("$between requires exactly 2 values");
        }
        this.expression = (Literal) expression;
    }
}
