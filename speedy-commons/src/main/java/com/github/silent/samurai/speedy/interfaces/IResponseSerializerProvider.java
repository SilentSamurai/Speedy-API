package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;

/// SPI contract for discovering response serializer implementations.
///
/// Analogous to {@link com.github.silent.samurai.speedy.interfaces.query.QueryProcessorProvider}.
/// Register implementations via {@code META-INF/services}.
public interface IResponseSerializerProvider {

    /// The MIME content type this provider handles (e.g. "application/json").
    String getContentType();

    /// Creates a serializer backed by the conversion context.
    ///
    /// The serializer is entity-agnostic: per-call data (including the target
    /// {@link EntityMetadata}) travels inside each {@link SpeedyResponse}, so a single
    /// instance handles entity lists, counts, batches, errors, and metadata alike.
    IResponseSerializerV2 create(MetaModel metaModel, ConversionContext context);
}
