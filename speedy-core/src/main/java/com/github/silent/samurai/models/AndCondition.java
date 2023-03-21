package com.github.silent.samurai.models;

import lombok.Data;

import java.util.List;

@Data
public class AndCondition implements Condition {

    private String operator;
    private List<Condition> conditions;

}
