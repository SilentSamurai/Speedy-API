package com.github.silent.samurai.speedy.json;

import com.github.silent.samurai.speedy.interfaces.request.ISpeedyIoProvider;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyRequestReader;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponseWriter;
import com.github.silent.samurai.speedy.json.request.JsonStructureReader;
import com.github.silent.samurai.speedy.json.response.JsonResponseWriter;

/// Unified SPI provider for all JSON I/O.
///
/// Implements the {@link ISpeedyIoProvider} contract:
/// - Response writer ({@link JsonResponseWriter})
/// - Request reader: the {@code byte[] -> StructureReader} factory {@link JsonStructureReader#over}
///
/// Discovered via ServiceLoader from {@code META-INF/services/...ISpeedyIoProvider}.
public class JsonSpeedyProvider implements ISpeedyIoProvider {

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public SpeedyResponseWriter createWriter() {
        return new JsonResponseWriter();
    }

    @Override
    public SpeedyRequestReader createReader() {
        return JsonStructureReader::over;
    }
}
