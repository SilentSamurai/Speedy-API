package com.github.silent.samurai.speedy.interfaces.backend;

import com.github.silent.samurai.speedy.interfaces.request.ISpeedyIoProvider;

/// The thin "dumb port" a persistence backend implements, bundling the read, write, and session
/// halves. Analogous to {@link ISpeedyIoProvider}
/// exposing a writer + reader: a backend module (jOOQ today; raw JDBC / R2DBC / Mongo later)
/// supplies one of these and the engine wraps it in the shared
/// {@code DefaultQueryProcessor} (in speedy-core), reusing all the
/// orchestration and tree-walking.
public interface SpeedyBackend extends RowReader, RowWriter, BackendSession {
}
