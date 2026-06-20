package com.github.silent.samurai.speedy.jooq.impl;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessorProvider;
import com.github.silent.samurai.speedy.interfaces.query.backend.SpeedyBackend;
import com.github.silent.samurai.speedy.jooq.impl.conversion.Converter;
import com.github.silent.samurai.speedy.jooq.impl.query.JooqBackend;
import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.query.walker.WalkingQueryProcessor;

import javax.sql.DataSource;

/// SPI provider for QueryProcessor. Discovered via ServiceLoader.
///
/// Wires the jOOQ {@link SpeedyBackend} port — which owns all value conversion (its
/// {@link Converter}) — into the shared, backend-agnostic {@link WalkingQueryProcessor},
/// which owns CRUD orchestration and entity-tree walking. DB value conversion is entirely
/// internal to this module, so {@code context} is not consulted here.
public class JooqQueryProcessorProvider implements QueryProcessorProvider {

    @Override
    public QueryProcessor create(DataSource dataSource, SpeedyDialect dialect, ConversionContext context) {
        Converter converter = JooqConverters.defaults();
        SpeedyBackend backend = new JooqBackend(dataSource, dialect, converter);
        return new WalkingQueryProcessor(backend);
    }
}
