package com.github.silent.samurai.speedy.interfaces.request;

import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponseWriter;

/// Unified SPI for format-level I/O providers (JSON, YAML, XML, etc.).
///
/// Exposes two format-specific pieces: the {@link SpeedyResponseWriter} sink (write
/// side) and the {@link SpeedyRequestReader} source (read side). The format-agnostic
/// {@code WalkingResponseSerializer} and {@code WalkingRequestParser} are constructed
/// by the engine, not the provider.
///
/// Register implementations via {@code META-INF/services/...ISpeedyIoProvider}.
public interface ISpeedyIoProvider {

    /// The MIME content type this provider handles (e.g. "application/json").
    String getContentType();

    /// Creates a format-specific response writer. The engine wraps it in the
    /// shared {@code WalkingResponseSerializer} that owns envelope composition.
    SpeedyResponseWriter createWriter();

    /// Creates a format-specific request reader: a {@code byte[] -> StructureReader} factory.
    /// The engine wraps it in the shared {@code WalkingRequestParser} that owns envelope
    /// composition and the {@code $query} parse.
    SpeedyRequestReader createReader();
}
