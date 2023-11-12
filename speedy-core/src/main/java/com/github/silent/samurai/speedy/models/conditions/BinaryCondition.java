package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.models.Operator;

public interface BinaryCondition extends Condition {

    DbField getField();

    Operator getOperator();
}
