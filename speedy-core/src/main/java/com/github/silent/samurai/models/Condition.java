package com.github.silent.samurai.models;

import com.fasterxml.jackson.core.JsonParser;


public interface Condition {
    String getOperator();

    void updateFromJson(JsonParser jsonParser);
}
