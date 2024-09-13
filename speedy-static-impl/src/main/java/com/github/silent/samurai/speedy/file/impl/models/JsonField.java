package com.github.silent.samurai.speedy.file.impl.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class JsonField {

    @JsonProperty(required = true)
    @NotNull(message = "Name cannot be null")
    public String name;

    @JsonProperty(required = true)
    @NotNull(message = "outputProperty cannot be null")
    public String outputProperty;

    @JsonProperty(required = true)
    @NotNull(message = "dbColumn cannot be null")
    public String dbColumn;

    @JsonProperty(required = true)
    @NotNull(message = "fieldType cannot be null")
    public String fieldType;

    public boolean isNullable = false;
    public boolean isAssociation = false;
    public boolean isCollection = false;
    public boolean isSerializable = true;
    public boolean isDeserializable = true;
    public boolean isUnique = false;
    public boolean isUpdatable = true;
    public boolean isInsertable = true;
    public boolean isRequired = false;
    public boolean isKeyField = false;

}
