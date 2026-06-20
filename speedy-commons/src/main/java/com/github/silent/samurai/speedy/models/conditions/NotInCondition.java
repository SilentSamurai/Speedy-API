package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.Literal;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import lombok.Getter;

@Getter
public class NotInCondition implements BinaryCondition {

    private final QueryField field;
    private final ConditionOperator operator = ConditionOperator.NOT_IN;
    private final Literal expression;

    public NotInCondition(QueryField field, Expression expression) throws SpeedyHttpException {
        this.field = field;
        if (!(expression instanceof Literal)) {
            throw new BadRequestException("NotIn only accepts a literal");
        }
        this.expression = (Literal) expression;
    }
}
