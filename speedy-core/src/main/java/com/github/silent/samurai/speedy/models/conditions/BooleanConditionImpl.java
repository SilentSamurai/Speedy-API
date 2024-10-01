package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class BooleanConditionImpl implements BooleanCondition {

    private final ConditionOperator operator;
    private final List<Condition> conditions = new LinkedList<>();

    public BooleanConditionImpl(ConditionOperator operator) {
        this.operator = operator;
    }

    @Override
    public void addSubCondition(Condition condition) {
        conditions.add(condition);
    }
}
