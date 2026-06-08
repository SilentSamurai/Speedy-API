package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;

import javax.sql.DataSource;

@FunctionalInterface
public interface QueryProcessorProvider {
    QueryProcessor create(DataSource dataSource, SpeedyDialect dialect);
}
