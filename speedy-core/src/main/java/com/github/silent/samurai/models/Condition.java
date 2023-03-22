package com.github.silent.samurai.models;

import com.google.gson.JsonElement;

public interface Condition {
    String getOperator();

    void updateFromJson(JsonElement jsonElement);
}
