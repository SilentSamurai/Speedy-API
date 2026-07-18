package com.github.silent.samurai.speedy.file.impl.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonField {

    @JsonProperty(required = true)
    public String name;

    @JsonProperty(required = true)
    public String outputProperty;

    @JsonProperty(required = true)
    public String dbColumn;

    @JsonProperty(required = true)
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

    public String associatedColumn;

    public Boolean sensitive;

}
