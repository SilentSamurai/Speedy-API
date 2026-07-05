package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;

public interface QueryField {

    FieldMetadata getFieldMetadata();

    boolean isAssociated();

    FieldMetadata getAssociatedFieldMetadata();


    FieldMetadata getMetadataForParsing();
}
