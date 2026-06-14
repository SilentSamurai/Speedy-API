package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;

/// Unified SPI for format-level I/O providers (JSON, YAML, XML, etc.).
///
/// Exposes two format-specific pieces: the {@link SpeedyResponseWriter} factory
/// and the {@link IRequestBodyParser} factory. The format-agnostic
/// {@code WalkingResponseSerializer} is constructed by the engine, not the provider.
///
/// Register implementations via {@code META-INF/services/...ISpeedyIoProvider}.
public interface ISpeedyIoProvider {

    /// The MIME content type this provider handles (e.g. "application/json").
    String getContentType();

    /// Creates a format-specific response writer. The engine wraps it in the
    /// shared {@code WalkingResponseSerializer} that owns envelope composition.
    SpeedyResponseWriter createWriter();

    /// Creates a request-body parser backed by the conversion context.
    IRequestBodyParser createParser(ConversionContext context);

    /// Contributes format-specific type registries to the conversion context.
    void contributeModule(ConversionContext ctx);
}
