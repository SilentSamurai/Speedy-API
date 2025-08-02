package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.metadata.MetaModelBuilder;

public interface MetaModelProcessor {

    MetaModel getMetaModel();

    void processMetaModel(MetaModelBuilder builder);

}
