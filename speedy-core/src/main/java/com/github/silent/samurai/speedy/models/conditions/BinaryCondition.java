package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.enums.ConditionOperator;

public interface BinaryCondition extends Condition {

    DbField getField();

    ConditionOperator getOperator();
}
