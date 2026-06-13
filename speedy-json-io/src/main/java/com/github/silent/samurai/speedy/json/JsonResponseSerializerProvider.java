package com.github.silent.samurai.speedy.json;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.json.registry.JsonRegistry;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.json.response.JSONSerializerV2;

/// SPI provider for JSON response serialization. Discovered via ServiceLoader.
public class JsonResponseSerializerProvider implements IResponseSerializerProvider {

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public IResponseSerializerV2 create(MetaModel metaModel, EntityMetadata entityMetadata, ConversionContext context) {
        return new JSONSerializerV2(metaModel, entityMetadata, context.get(JsonRegistry.class));
    }
}
