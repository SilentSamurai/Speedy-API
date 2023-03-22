package com.github.silent.samurai.models;

import com.github.silent.samurai.query.QueryDeserializer;
import com.github.silent.samurai.query.QuerySerializer;
import com.google.gson.GsonBuilder;
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
        EqCondition eqCondition = new EqCondition();
        eqCondition.setOperator("=");
        eqCondition.setField("name");
        eqCondition.setValue("oli");
        query.setWhere(Arrays.asList(eqCondition, eqCondition));
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Condition.class, new QueryDeserializer());
        gsonBuilder.registerTypeAdapter(Condition.class, new QuerySerializer());
        String jsonString = gsonBuilder.create().toJson(query);
        logger.info(jsonString);
        Query generatedQuery = gsonBuilder.create().fromJson(jsonString, Query.class);
        assert generatedQuery.getWhere().get(0) instanceof EqCondition;
    }
}