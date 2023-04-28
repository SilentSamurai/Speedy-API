package com.github.silent.samurai.speedy.model;

import lombok.Data;

@Data
public class Filter {

    private String operator;
    private String identifier;
    private FilterValue value;

}
