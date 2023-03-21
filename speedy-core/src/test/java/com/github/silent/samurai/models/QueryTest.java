package com.github.silent.samurai.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class QueryTest {

    Logger logger = LogManager.getLogger(QueryTest.class);

    @BeforeEach
    void setUp() {
    }

    @Test
    void getAggregation() {

        Query query = new Query();
        query.setWhere(Arrays.asList(new EqCondition(), new EqCondition()));

        GsonBuilder gsonBuilder = new GsonBuilder();
        JsonDeserializer<Condition> conditionJsonSerializer = (jsonElement, type, context) -> {
            if (jsonElement.isJsonArray()) {
                return new AndCondition();
            } else {
                return new EqCondition();
            }
        };
        gsonBuilder.registerTypeAdapter(Condition.class, conditionJsonSerializer);

        String jsonString = gsonBuilder.create().toJson(query);

        logger.info(jsonString);

        Query generatedQuery = gsonBuilder.create().fromJson(jsonString, Query.class);

        assert generatedQuery.getWhere().get(0) instanceof EqCondition;


    }
}