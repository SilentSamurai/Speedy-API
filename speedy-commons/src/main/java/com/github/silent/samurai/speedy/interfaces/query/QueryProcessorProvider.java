package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;

import javax.sql.DataSource;

@FunctionalInterface
public interface QueryProcessorProvider {
    QueryProcessor create(DataSource dataSource, SpeedyDialect dialect, ConversionContext context);
}
