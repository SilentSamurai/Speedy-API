package com.github.silent.samurai.speedy.model;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class FilterValue {

    private List<String> values = new LinkedList<>();
    private boolean multiple;


    public void addValue(String value) {
        if (!multiple) {
            multiple = values.size() + 1 > 1;
        }
        values.add(value);
    }

}
