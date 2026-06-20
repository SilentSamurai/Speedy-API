package com.github.silent.samurai.speedy.jooq.impl;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessorFactory;
import com.github.silent.samurai.speedy.interfaces.query.backend.SpeedyBackend;
import com.github.silent.samurai.speedy.jooq.impl.query.JooqBackend;
import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;

import javax.sql.DataSource;

/// SPI factory for the jOOQ {@link SpeedyBackend}. Discovered via ServiceLoader.
///
/// Returns a bare backend — no wrapping in an orchestration layer (that happens
/// in {@code speedy-core}). Only {@code speedy-commons} is needed at compile time.
public class JooqBackendFactory implements QueryProcessorFactory {

    @Override
    public SpeedyBackend create(DataSource dataSource, SpeedyDialect dialect, ConversionContext context) {
        return new JooqBackend(dataSource, dialect, JooqConverters.defaults());
    }
}
