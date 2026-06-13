package com.github.silent.samurai.speedy.json;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.json.registry.JsonRegistry;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParserProvider;
import com.github.silent.samurai.speedy.json.request.JSONBodyParser;

/// SPI provider for JSON request-body parsing. Discovered via ServiceLoader.
public class JsonBodyParserProvider implements IRequestBodyParserProvider {

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public IRequestBodyParser create(ConversionContext context) {
        return new JSONBodyParser(context.get(JsonRegistry.class));
    }
}
