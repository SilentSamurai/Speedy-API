package com.github.silent.samurai.speedy.interfaces.query;


public interface BinaryCondition extends Condition {

    Field getField();

    SpeedyValue getSpeedyValue();
}
