package com.github.silent.samurai.speedy.models;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class Filter {

    private String internalId;
    private String field;
    private String operator;
    private String associationId;
    private List<String> values = new LinkedList<>();
    private boolean multiple;
    private boolean associationPresent = false;

    public void setAssociationId(String associationId) {
        this.associationId = associationId;
        this.associationPresent = true;
    }

    public void addValue(String value) {
        if (!multiple) {
            multiple = values.size() + 1 > 1;
        }
        values.add(value);
    }


}
