package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import lombok.Getter;

@Getter
public class LessThanEqualCondition implements BinaryCondition {

    private final QueryField field;
    private final SpeedyValue speedyValue;
    private final ConditionOperator operator = ConditionOperator.LTE;


    public LessThanEqualCondition(QueryField field, SpeedyValue speedyValue) {
        this.field = field;
        this.speedyValue = speedyValue;
    }
}
