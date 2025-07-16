package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GreaterThanCondition implements BinaryCondition {

    private final QueryField field;
    private final Expression expression;
    private final ConditionOperator operator = ConditionOperator.GT;

    public GreaterThanCondition(QueryField field, Expression expression) {
        this.field = field;
        this.expression = expression;
    }
}
