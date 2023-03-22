package com.github.silent.samurai.query;

import com.github.silent.samurai.models.AndCondition;
import com.github.silent.samurai.models.Condition;
import com.github.silent.samurai.models.EqCondition;
import com.github.silent.samurai.models.OrCondition;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class QueryDeserializer implements JsonDeserializer<Condition> {
    @Override
    public Condition deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        if (jsonElement.isJsonArray()) {
            return new AndCondition();
        }
        JsonObject currentElement = jsonElement.getAsJsonObject();
        Condition condition = null;
        if (currentElement.has("operator")) {
            String operator = currentElement.get("operator").getAsString();
            if (operator.equals("=")) {
                condition = new EqCondition();
                condition.updateFromJson(currentElement);
            }
            if (operator.equals("OR")) {
                OrCondition orCondition = new OrCondition();
                TypeToken<List<Condition>> listTypeToken = new TypeToken<>() {
                };
                List<Condition> conditions = context.deserialize(currentElement.get("conditions"), listTypeToken.getType());
                orCondition.setConditions(conditions);
            }
        }
        return condition;
    }
}
