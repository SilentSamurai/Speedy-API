package com.github.silent.samurai.models;

import lombok.Data;

import java.util.List;

@Data
public class OrCondition implements Condition {

    private String operator;
    private List<Condition> conditions;

}
