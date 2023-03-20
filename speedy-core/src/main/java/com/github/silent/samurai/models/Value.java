package com.github.silent.samurai.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Value {

    @SerializedName("start")
    @Expose
    public String start;
    @SerializedName("end")
    @Expose
    public String end;

}
