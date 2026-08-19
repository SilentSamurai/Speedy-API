package com.github.silent.samurai.speedy.file.impl.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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

    /// Single-column foreign key: the target entity's key field this association binds to (the
    /// local column is the field's own {@code dbColumn}). Mutually exclusive with
    /// {@link #associatedColumns}.
    public String associatedColumn;

    /// Multi-column foreign key for a target with a composite primary key: one entry per key
    /// column, in the target's key-field order. Mutually exclusive with {@link #associatedColumn}.
    public List<JsonAssociationColumn> associatedColumns;

    public Boolean sensitive;

}
