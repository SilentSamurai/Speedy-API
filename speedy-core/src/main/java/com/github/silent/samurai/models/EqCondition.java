package com.github.silent.samurai.models;

import com.fasterxml.jackson.core.JsonParser;
import lombok.Data;

@Data
public class EqCondition implements BinaryCondition {

    private String field;
    private String operator;
    private String value;

    @Override
    public void updateFromJson(JsonParser jsonParser) {
    }
}
