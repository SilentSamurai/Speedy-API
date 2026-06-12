package com.github.silent.samurai.speedy.jooq.impl;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessorProvider;
import com.github.silent.samurai.speedy.mappings.ConversionContext;
import com.github.silent.samurai.speedy.mappings.DbConversionRegistry;

import javax.sql.DataSource;

/// SPI provider for QueryProcessor. Discovered via ServiceLoader.
public class JooqQueryProcessorProvider implements QueryProcessorProvider {

    @Override
    public QueryProcessor create(DataSource dataSource, SpeedyDialect dialect, ConversionContext context) {
        DbConversionRegistry userDb = context.has(DbConversionRegistry.class)
                ? context.get(DbConversionRegistry.class)
                : null;
        DbConversionRegistry registry = JooqConverters.defaults(userDb);
        return new JooqQueryProcessorImpl(dataSource, dialect, registry);
    }
}
