package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import lombok.Getter;

@Getter
public class AssociatedField implements QueryField {

    private final FieldMetadata fieldMetadata;
    private final FieldMetadata associatedFieldMetadata;
    private final boolean associated;

    public AssociatedField(FieldMetadata fieldMetadata, FieldMetadata associatedFieldMetadata) {
        this.fieldMetadata = fieldMetadata;
        this.associatedFieldMetadata = associatedFieldMetadata;
        this.associated = true;
    }

    @Override
    public FieldMetadata getMetadataForParsing() {
        return getAssociatedFieldMetadata();
    }
}
