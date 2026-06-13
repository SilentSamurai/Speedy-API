package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;

/// SPI contract for discovering response serializer implementations.
///
/// Analogous to {@link com.github.silent.samurai.speedy.interfaces.query.QueryProcessorProvider}.
/// Register implementations via {@code META-INF/services}.
public interface IResponseSerializerProvider {

    /// The MIME content type this provider handles (e.g. "application/json").
    String getContentType();

    /// Creates a new serializer instance for the given entity, backed by the conversion context.
    IResponseSerializerV2 create(MetaModel metaModel, EntityMetadata entityMetadata, ConversionContext context);
}
