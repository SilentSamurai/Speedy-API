package com.github.silent.samurai.models;

import com.google.gson.JsonElement;
import lombok.Data;

@Data
public class EqCondition implements BinaryCondition {

    private String field;
    private String operator;
    private String value;

    @Override
    public void updateFromJson(JsonElement jsonElement) {
        this.setField(jsonElement.getAsJsonObject().get("field").getAsString());
        this.setValue(jsonElement.getAsJsonObject().get("value").getAsString());
        this.setOperator(jsonElement.getAsJsonObject().get("operator").getAsString());
    }
}
