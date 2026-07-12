package com.github.silent.samurai.speedy.policy.condition;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Condition;

import java.util.Map;
import java.util.Optional;

public class QueryCondition implements PolicyCondition {

    public static final String TYPE = "QueryCondition";

    private final Map<String, Object> rawSpec;
    private Condition parsedCondition;
    private EntityMetadata parsedFor;

    public QueryCondition(Map<String, Object> rawSpec) {
        this.rawSpec = Map.copyOf(rawSpec);
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public boolean isSatisfied(ConditionContext ctx) {
        try {
            Condition condition = ensureParsed(ctx.entityMetadata());
            return ConditionEvaluator.evaluate(condition, ctx);
        } catch (SpeedyHttpException e) {
            return false;
        }
    }

    @Override
    public Optional<Condition> toQueryCondition(ConditionContext ctx) {
        try {
            Condition condition = ensureParsed(ctx.entityMetadata());
            Map<String, SpeedyValue> variables = ctx.variables();
            if (variables != null && !variables.isEmpty()) {
                condition = ConditionResolver.resolve(condition, variables);
            }
            if (condition instanceof BooleanCondition bc
                    && bc.getOperator() == ConditionOperator.AND
                    && bc.getConditions().size() == 1) {
                condition = bc.getConditions().get(0);
            }
            return Optional.of(condition);
        } catch (SpeedyHttpException e) {
            return Optional.empty();
        }
    }

    private Condition ensureParsed(EntityMetadata entityMetadata) throws SpeedyHttpException {
        if (parsedCondition == null || parsedFor != entityMetadata) {
            parsedCondition = ConditionJsonParser.parse(rawSpec, entityMetadata);
            parsedFor = entityMetadata;
        }
        return parsedCondition;
    }
}
