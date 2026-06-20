package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;

/// # Handler
///
/// Contract for a single processing step in the Speedy request pipeline.
/// Handlers are aggregated into sub-chains ({@code List<Handler>}) that are
/// iterated sequentially via a {@code for} loop in {@code SpeedyEngineImpl.run()}.
/// Handlers do <b>not</b> hold a {@code next} reference.
///
/// ## Sub-Chain Organization
/// Sub-chains are wired in {@code SpeedyEngineImpl} for each lifecycle phase:
/// URI parsing, header parsing, operation resolution, parser/body handling,
/// serializer selection, and each CRUD operation type. Response writing is
/// performed directly in {@code SpeedyFactory.processReqV2()}, not via a handler.
///
/// @see com.github.silent.samurai.speedy.handlers package summary
public interface Handler {
    void process(SpeedyContext context) throws SpeedyHttpException;
}
