package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import lombok.Data;

@Data
public class LessThanCondition implements BinaryCondition {

    private final QueryField field;
    private final SpeedyValue speedyValue;
    private final ConditionOperator operator = ConditionOperator.LT;

    public LessThanCondition(QueryField field, SpeedyValue speedyValue) {
        this.field = field;
        this.speedyValue = speedyValue;
    }
}
