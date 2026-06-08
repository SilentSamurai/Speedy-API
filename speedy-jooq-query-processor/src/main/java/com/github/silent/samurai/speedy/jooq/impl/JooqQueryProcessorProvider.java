package com.github.silent.samurai.speedy.jooq.impl;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessorProvider;

import javax.sql.DataSource;

/// SPI provider for QueryProcessor. Discovered via ServiceLoader.
public class JooqQueryProcessorProvider implements QueryProcessorProvider {

    @Override
    public QueryProcessor create(DataSource dataSource, SpeedyDialect dialect) {
        return new JooqQueryProcessorImpl(dataSource, dialect);
    }
}
