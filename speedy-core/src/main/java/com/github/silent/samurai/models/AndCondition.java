package com.github.silent.samurai.models;

import com.google.gson.JsonElement;
import lombok.Data;

import java.util.List;

@Data
public class AndCondition implements Condition {

    private String operator;
    private List<Condition> conditions;

    @Override
    public void updateFromJson(JsonElement jsonElement) {

    }
}
