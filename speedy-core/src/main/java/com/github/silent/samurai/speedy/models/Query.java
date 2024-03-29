package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.models.conditions.Condition;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

@Data

public class Query {

    @Valid
    public List<Aggregation> aggregation;

    @Valid
    public List<Condition> where;

    @Valid
    public List<String> groupBy;

    @Valid
    public List<Condition> having;

    @Valid
    public List<String> orderBy;

}
