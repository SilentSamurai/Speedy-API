package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import lombok.Getter;

@Getter
public class NormalField implements QueryField {

    private final FieldMetadata fieldMetadata;
    private final boolean associated;

    public NormalField(FieldMetadata fieldMetadata) {
        this.fieldMetadata = fieldMetadata;
        this.associated = false;
    }

    @Override
    public FieldMetadata getAssociatedFieldMetadata() {
        throw new RuntimeException("NormalField does not have associated field");
    }

    @Override
    public FieldMetadata getMetadataForParsing() {
        return getFieldMetadata();
    }
}
