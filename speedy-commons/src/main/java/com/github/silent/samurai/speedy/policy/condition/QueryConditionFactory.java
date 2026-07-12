package com.github.silent.samurai.speedy.policy.condition;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueryConditionFactory implements PolicyConditionFactory {

    @Override
    public String type() {
        return QueryCondition.TYPE;
    }

    @Override
    public PolicyCondition parse(ConditionSpec spec) {
        Map<String, Object> raw = new LinkedHashMap<>(spec.asMap());
        raw.remove("type");
        return new QueryCondition(raw);
    }
}
