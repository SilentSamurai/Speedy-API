package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;

/// Format bundle returned by {@link ISpeedyIoProvider#createReader}: the read-side
/// counterpart of {@link SpeedyResponseWriter}. Produces a streaming
/// {@link StructureReader} over the raw body for entity-tree parsing, plus the
/// still-format-specific {@code $query} body parse (which remains tree-based and is
/// out of the unified entity-tree path). The engine wraps this in the shared
/// {@code WalkingRequestParser} that owns envelope composition.
public interface SpeedyRequestReader {

    /// Opens a streaming token reader over the raw request body.
    StructureReader readDocument(byte[] rawBody) throws SpeedyHttpException;

    /// Parses a {@code $query} body into a {@link SpeedyQuery} (format-specific).
    SpeedyQuery parseQuery(byte[] rawBody, MetaModel metaModel, SpeedyQuery baseQuery,
                           int maxPageSize, int defaultPageSize) throws SpeedyHttpException;
}
