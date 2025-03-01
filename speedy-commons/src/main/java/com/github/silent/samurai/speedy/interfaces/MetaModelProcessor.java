package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.metadata.MetadataBuilder;

public interface MetaModelProcessor {

    MetaModel getMetaModel();

    void processMetaModel(MetadataBuilder.MetaModelBuilder builder);

}
