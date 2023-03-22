package com.github.silent.samurai.query;

import com.github.silent.samurai.models.Condition;
import com.github.silent.samurai.models.EqCondition;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class QuerySerializer implements JsonSerializer<Condition> {
    @Override
    public JsonElement serialize(Condition condition, Type type, JsonSerializationContext context) {
        if (condition.getOperator().equals("=")) {
            return context.serialize(condition, EqCondition.class);
        }
        return null;
    }
}
