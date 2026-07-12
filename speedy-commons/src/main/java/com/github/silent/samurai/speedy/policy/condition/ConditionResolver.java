package com.github.silent.samurai.speedy.policy.condition;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.Literal;
import com.github.silent.samurai.speedy.interfaces.query.VariableRef;
import com.github.silent.samurai.speedy.models.conditions.BinaryConditionImpl;
import com.github.silent.samurai.speedy.models.conditions.BooleanConditionImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ConditionResolver {

    private ConditionResolver() {
    }

    public static Condition resolve(Condition condition, Map<String, SpeedyValue> variables)
            throws SpeedyHttpException {
        if (condition instanceof BinaryCondition c) {
            return resolveBinary(c, variables);
        }
        if (condition instanceof BooleanCondition c) {
            return resolveBoolean(c, variables);
        }
        return condition;
    }

    private static Condition resolveBinary(BinaryCondition c, Map<String, SpeedyValue> variables)
            throws SpeedyHttpException {
        Expression resolved = resolveExpression(c.getExpression(), variables);
        if (resolved == c.getExpression()) {
            return c;
        }
        return new BinaryConditionImpl(c.getField(), c.getOperator(), resolved);
    }

    private static Condition resolveBoolean(BooleanCondition c, Map<String, SpeedyValue> variables)
            throws SpeedyHttpException {
        List<Condition> resolved = new ArrayList<>();
        boolean changed = false;
        for (Condition sub : c.getConditions()) {
            Condition r = resolve(sub, variables);
            resolved.add(r);
            if (r != sub) {
                changed = true;
            }
        }
        if (!changed) {
            return c;
        }
        BooleanConditionImpl group = new BooleanConditionImpl(c.getOperator());
        resolved.forEach(group::addSubCondition);
        return group;
    }

    private static Expression resolveExpression(Expression expr, Map<String, SpeedyValue> variables)
            throws BadRequestException {
        if (expr instanceof VariableRef vr) {
            if (!variables.containsKey(vr.name())) {
                throw new BadRequestException("Unresolvable variable reference: ${" + vr.name() + "}");
            }
            return new Literal(variables.get(vr.name()));
        }
        return expr;
    }
}
