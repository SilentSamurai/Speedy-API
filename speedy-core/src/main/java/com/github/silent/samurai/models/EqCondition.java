package com.github.silent.samurai.models;

import lombok.Data;

@Data
public class EqCondition implements BinaryCondition {

    private String field;
    private String operator;
    private String value;

}
