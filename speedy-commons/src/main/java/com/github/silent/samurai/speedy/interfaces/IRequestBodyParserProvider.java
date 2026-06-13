package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;

/// SPI contract for discovering request-body parser implementations.
///
/// Analogous to {@link com.github.silent.samurai.speedy.interfaces.query.QueryProcessorProvider}.
/// Register implementations via {@code META-INF/services}.
public interface IRequestBodyParserProvider {

    /// The MIME content type this provider handles (e.g. "application/json").
    String getContentType();

    /// Creates a new parser instance backed by the given conversion context.
    IRequestBodyParser create(ConversionContext context);
}
