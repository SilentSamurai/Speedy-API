package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;

/// Unified SPI for format-level I/O providers (JSON, YAML, XML, etc.).
///
/// Merges the three previous interfaces into a single contract:
/// - {@link IResponseSerializerV2} factory
/// - {@link IRequestBodyParser} factory
/// - type-module contribution (formerly {@code SpeedyTypeModule})
///
/// Register implementations via {@code META-INF/services/...ISpeedyIoProvider}.
public interface ISpeedyIoProvider {

    /// The MIME content type this provider handles (e.g. "application/json").
    String getContentType();

    /// Creates a response serializer backed by the conversion context.
    IResponseSerializerV2 createSerializer(MetaModel metaModel, ConversionContext context);

    /// Creates a request-body parser backed by the conversion context.
    IRequestBodyParser createParser(ConversionContext context);

    /// Contributes format-specific type registries to the conversion context.
    void contributeModule(ConversionContext ctx);
}
