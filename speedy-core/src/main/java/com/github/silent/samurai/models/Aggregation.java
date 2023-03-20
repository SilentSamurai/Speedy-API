package com.github.silent.samurai.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Aggregation {

    @SerializedName("fn")
    @Expose
    public String fn;
    @SerializedName("args")
    @Expose
    public String args;
    @SerializedName("alias")
    @Expose
    public String alias;

}
