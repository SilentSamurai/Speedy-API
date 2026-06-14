package com.github.silent.samurai.speedy.json;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.json.registry.JsonRegistry;
import com.github.silent.samurai.speedy.json.request.JSONBodyParser;
import com.github.silent.samurai.speedy.json.response.JSONResponseSerializer;
import org.springframework.http.MediaType;

/// Unified SPI provider for all JSON I/O.
///
/// Implements the three-role {@link ISpeedyIoProvider} contract:
/// - response serializer factory ({@link JSONResponseSerializer})
/// - request body parser factory ({@link JSONBodyParser})
/// - type-module contributor ({@link JsonRegistry})
///
/// Discovered via ServiceLoader from {@code META-INF/services/...ISpeedyIoProvider}.
public class JsonSpeedyProvider implements ISpeedyIoProvider {

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public IResponseSerializerV2 createSerializer(MetaModel metaModel, ConversionContext context) {
        return new JSONResponseSerializer(context.get(JsonRegistry.class));
    }

    @Override
    public IRequestBodyParser createParser(ConversionContext context) {
        return new JSONBodyParser(context.get(JsonRegistry.class));
    }

    @Override
    public void contributeModule(ConversionContext ctx) {
        ctx.put(JsonRegistry.defaults());
    }
}
