package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.interfaces.backend.SpeedyBackend;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessorFactory;
import com.github.silent.samurai.speedy.conversion.ext.SpeedyTypeModule;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModelProcessor;
import com.github.silent.samurai.speedy.policy.SpeedyAuthContext;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public interface ISpeedyConfiguration {

    MetaModelProcessor metaModelProcessor();

    void register(ISpeedyRegistry registry);

    DataSource dataSourcePerReq();

    SpeedyDialect getDialect();

    /**
     * Resolves the access-control policy for the current request, mirroring how
     * {@link #dataSourcePerReq()} resolves the current request's {@link DataSource}: the
     * implementation is free to read ambient request state (e.g. via a {@code RequestContextHolder}
     * or similar) and decode a caller's token however it likes — Speedy does not care where the
     * policy comes from, only that it is scoped to the current caller.
     * <p>
     * An empty {@link Optional} (the default) is interpreted by the default engine as an empty,
     * deny-by-default policy document. Return an explicit {@link SpeedyAuthContext} containing a
     * {@code PolicyEffect.ALLOW} default document for a request that should be unrestricted.
     */
    default Optional<SpeedyAuthContext> authContextPerReq() {
        return Optional.empty();
    }

    default boolean isMetadataEndpointEnabled() {
        return true;
    }

    default int getDefaultPageSize() {
        return 20;
    }

    default int getMaxPageSize() {
        return 1000;
    }

    default int getMaxQueryStringLength() {
        return 2048;
    }

    default int getMaxFilterCount() {
        return 100;
    }

    /**
     * Maximum nesting depth of boolean ({@code AND}/{@code OR}) groups allowed in a
     * query's {@code WHERE} clause. Guards against pathologically nested queries.
     */
    default int getMaxConditionDepth() {
        return 5;
    }

    /**
     * Maximum number of {@code $expand} entries allowed in a single query.
     */
    default int getMaxExpandCount() {
        return 10;
    }

    default long getMaxRequestBodySize() {
        return 1_048_576;
    }

    default SpeedyBackend queryBackend(DataSource dataSource, SpeedyDialect dialect) {
        return ServiceLoader.load(QueryProcessorFactory.class)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(
                        "No QueryProcessorFactory implementation on classpath. " +
                                "Add 'speedy-jooq-query-processor' dependency or override queryBackend() in ISpeedyConfiguration."))
                .create(dataSource, dialect);
    }

    default List<SpeedyTypeModule> typeModules() {
        return List.of();
    }

}
