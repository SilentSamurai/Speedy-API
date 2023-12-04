package com.github.silent.samurai.speedy.interfaces.query;

import java.util.List;

public interface BooleanCondition extends Condition {

    List<Condition> getConditions();

    void addSubCondition(Condition condition);
}
