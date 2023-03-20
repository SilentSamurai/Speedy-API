package com.github.silent.samurai.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Having {

    @SerializedName("operator")
    @Expose
    public String operator;
    @SerializedName("field")
    @Expose
    public String field;
    @SerializedName("value")
    @Expose
    public String value;

}
