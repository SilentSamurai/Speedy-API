package com.github.silent.samurai.speedy.file.impl.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.silent.samurai.speedy.enums.IgnoreType;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class FileFieldMetadata implements FieldMetadata {

    private String name;
    private ValueType valueType;
    private String outputPropertyName;
    private String dbColumnName;
    private boolean isNullable;
    private boolean isAssociation;
    private boolean isCollection;
    private boolean isSerializable;
    private boolean isDeserializable;
    private boolean isUnique;
    private boolean isInsertable = true;
    private boolean isUpdatable = true;
    private boolean isRequired = true;
    private String type;
    private FileEntityMetadata entityMetadata;
    private FileEntityMetadata associationMetadata;


    @Override
    public IgnoreType getIgnoreProperty() {
        return null;
    }

    @Override
    public Class<?> getFieldType() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileFieldMetadata that = (FileFieldMetadata) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
