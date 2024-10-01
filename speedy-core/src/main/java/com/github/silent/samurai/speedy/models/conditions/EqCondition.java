package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EqCondition implements BinaryCondition {

    private final QueryField field;
    private final SpeedyValue speedyValue;
    private final ConditionOperator operator = ConditionOperator.EQ;


    public EqCondition(QueryField queryField, SpeedyValue speedyValue) {
        this.field = queryField;
        this.speedyValue = speedyValue;
    }
}
