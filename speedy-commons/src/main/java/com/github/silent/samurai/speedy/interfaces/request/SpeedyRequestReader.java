package com.github.silent.samurai.speedy.interfaces.request;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponseWriter;

/// Format bundle returned by {@link ISpeedyIoProvider#createReader}: the read-side
/// counterpart of {@link SpeedyResponseWriter}. A {@code byte[] -> StructureReader} factory —
/// the shared {@code WalkingRequestParser} is built before the body is known, so it holds this
/// factory and opens a fresh streaming {@link StructureReader} per parse (for both entity-tree
/// and {@code $query} parsing). The format module is purely the streaming token port, with no
/// format-specific parsing logic; a format satisfies it with a method reference to its reader's
/// factory (e.g. {@code JsonStructureReader::over}).
@FunctionalInterface
public interface SpeedyRequestReader {

    /// Opens a streaming token reader over the raw request body.
    StructureReader readDocument(byte[] rawBody) throws SpeedyHttpException;
}
