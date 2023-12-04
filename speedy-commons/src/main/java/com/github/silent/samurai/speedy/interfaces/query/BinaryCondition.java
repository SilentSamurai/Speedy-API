package com.github.silent.samurai.speedy.interfaces.query;


public interface BinaryCondition extends Condition {

    QueryField getField();

    SpeedyValue getSpeedyValue();
}
