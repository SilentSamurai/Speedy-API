package com.github.silent.samurai.speedy.json;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.json.registry.JsonRegistry;
import org.springframework.http.MediaType;

/// SPI provider for JSON response serialization. Discovered via ServiceLoader.
public class JsonResponseSerializerProvider implements IResponseSerializerProvider {

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public IResponseSerializerV2 create(MetaModel metaModel, ConversionContext context) {
        return new JSONResponseSerializer(context.get(JsonRegistry.class));
    }
}
