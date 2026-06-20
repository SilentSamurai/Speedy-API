package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.interfaces.query.backend.SpeedyBackend;
import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;

import javax.sql.DataSource;

/// SPI for providing the {@link SpeedyBackend} implementation (jOOQ, R2DBC, etc.).
/// Discovered via {@link java.util.ServiceLoader} at runtime so the backend is
/// swappable by changing the classpath dependency.
@FunctionalInterface
public interface QueryProcessorFactory {
    SpeedyBackend create(DataSource dataSource, SpeedyDialect dialect, ConversionContext context);
}
