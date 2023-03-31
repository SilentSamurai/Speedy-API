package com.github.silent.samurai.models;

import com.github.silent.samurai.query.QueryDeserializer;
import com.github.silent.samurai.query.QuerySerializer;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

class QueryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryTest.class);

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
        LOGGER.info(jsonString);
        Query generatedQuery = gsonBuilder.create().fromJson(jsonString, Query.class);
        assert generatedQuery.getWhere().get(0) instanceof EqCondition;
    }
}