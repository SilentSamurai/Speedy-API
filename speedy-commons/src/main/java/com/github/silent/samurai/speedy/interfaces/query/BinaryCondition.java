package com.github.silent.samurai.speedy.interfaces.query;


import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

public interface BinaryCondition extends Condition {

    QueryField getField();

    SpeedyValue getSpeedyValue();
}
