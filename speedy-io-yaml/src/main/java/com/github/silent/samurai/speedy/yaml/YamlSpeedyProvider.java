package com.github.silent.samurai.speedy.yaml;

import com.github.silent.samurai.speedy.interfaces.request.ISpeedyIoProvider;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyRequestReader;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponseWriter;
import com.github.silent.samurai.speedy.yaml.request.YamlStructureReader;
import com.github.silent.samurai.speedy.yaml.response.YamlResponseWriter;

/// Unified SPI provider for all YAML I/O.
///
/// Implements the {@link ISpeedyIoProvider} contract:
/// - Response writer ({@link YamlResponseWriter})
/// - Request reader: the {@code byte[] -> StructureReader} factory {@link YamlStructureReader#over}
///
/// Discovered via ServiceLoader from {@code META-INF/services/...ISpeedyIoProvider}.
///
/// A near-verbatim port of the JSON provider: YAML's Jackson backend emits the same
/// streaming token model as JSON, so the reader/writer differ only in their {@code JsonFactory}.
public class YamlSpeedyProvider implements ISpeedyIoProvider {

    @Override
    public String getContentType() {
        return "application/yaml";
    }

    @Override
    public SpeedyResponseWriter createWriter() {
        return new YamlResponseWriter();
    }

    @Override
    public SpeedyRequestReader createReader() {
        return YamlStructureReader::over;
    }
}
