package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import lombok.Getter;

@Getter
public class LessThanEqualCondition implements BinaryCondition {

    private final QueryField field;
    private final Expression expression;
    private final ConditionOperator operator = ConditionOperator.LTE;


    public LessThanEqualCondition(QueryField field, Expression expression) {
        this.field = field;
        this.expression = expression;
    }
}
