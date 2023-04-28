package com.github.silent.samurai.models.conditions;

import com.github.silent.samurai.models.Operator;

public interface BinaryCondition extends Condition {

    String getField();

    Operator getOperator();
}
