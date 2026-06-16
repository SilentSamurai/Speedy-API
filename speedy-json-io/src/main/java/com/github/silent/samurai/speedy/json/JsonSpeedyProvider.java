package com.github.silent.samurai.speedy.json;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.json.request.JsonRequestReader;
import com.github.silent.samurai.speedy.json.response.JsonResponseWriter;
import org.springframework.http.MediaType;

/// Unified SPI provider for all JSON I/O.
///
/// Implements the {@link ISpeedyIoProvider} contract:
/// - Response writer factory ({@link JsonResponseWriter})
/// - Request reader factory ({@link JsonRequestReader})
///
/// Discovered via ServiceLoader from {@code META-INF/services/...ISpeedyIoProvider}.
public class JsonSpeedyProvider implements ISpeedyIoProvider {

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public SpeedyResponseWriter createWriter() {
        return new JsonResponseWriter();
    }

    @Override
    public SpeedyRequestReader createReader(ConversionContext context) {
        return new JsonRequestReader();
    }
}
