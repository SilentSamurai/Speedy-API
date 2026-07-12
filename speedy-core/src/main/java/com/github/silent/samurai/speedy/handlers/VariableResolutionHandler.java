package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import com.github.silent.samurai.speedy.policy.PolicyEngine;
import com.github.silent.samurai.speedy.policy.condition.ConditionResolver;
import com.github.silent.samurai.speedy.utils.ReadQueryResolver;

import java.util.Map;

public class VariableResolutionHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        PolicyEngine engine = context.find(PolicyEngine.class)
                .orElseThrow(() -> new InternalServerError("Policy engine is required"));
        Map<String, SpeedyValue> variables = engine.getVariables();
        if (variables.isEmpty()) {
            return;
        }
        SpeedyQuery query = ReadQueryResolver.resolve(context);
        BooleanCondition where = query.getWhere();
        if (where == null || where.getConditions().isEmpty()) {
            return;
        }
        Condition resolved = ConditionResolver.resolve(where, variables);
        if (resolved != where) {
            ((SpeedyQueryImpl) query).setWhere((BooleanCondition) resolved);
        }
    }
}
