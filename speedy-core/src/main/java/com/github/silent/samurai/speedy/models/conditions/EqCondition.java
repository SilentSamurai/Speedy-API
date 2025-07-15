package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import lombok.Getter;

@Getter
public class EqCondition implements BinaryCondition {

    private final QueryField field;
    private final Expression expression;
    private final ConditionOperator operator = ConditionOperator.EQ;

    public EqCondition(QueryField queryField, Expression expression) {
        this.field = queryField;
        this.expression = expression;
    }
}
