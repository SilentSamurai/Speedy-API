package com.github.silent.samurai.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

@Data

public class Query {

    @SerializedName("aggregation")
    @Expose
    @Valid
    public List<Aggregation> aggregation;

    @SerializedName("where")
    @Expose
    @Valid
    public List<Condition> where;

    @SerializedName("groupBy")
    @Expose
    @Valid
    public List<String> groupBy;

    @SerializedName("having")
    @Expose
    @Valid
    public List<Condition> having;

    @SerializedName("orderBy")
    @Expose
    @Valid
    public List<String> orderBy;

}
