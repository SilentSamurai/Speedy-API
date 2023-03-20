package com.github.silent.samurai.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

@Data
public class Where {

    @SerializedName("operator")
    @Expose
    public String operator;
    @SerializedName("field")
    @Expose
    public String field;
    @SerializedName("value")
    @Expose
    @Valid
    public Value value;
    @SerializedName("conditions")
    @Expose
    @Valid
    public List<Condition> conditions;

}
