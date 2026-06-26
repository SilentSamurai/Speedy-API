package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessorFactory;
import com.github.silent.samurai.speedy.interfaces.query.backend.SpeedyBackend;
import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.conversion.ext.SpeedyTypeModule;

import javax.sql.DataSource;
import java.util.List;
import java.util.ServiceLoader;

public interface ISpeedyConfiguration {

    MetaModelProcessor metaModelProcessor();

    void register(ISpeedyRegistry registry);

    DataSource dataSourcePerReq();

    SpeedyDialect getDialect();

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

    default SpeedyBackend queryBackend(DataSource dataSource, SpeedyDialect dialect, ConversionContext context) {
        return ServiceLoader.load(QueryProcessorFactory.class)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(
                        "No QueryProcessorFactory implementation on classpath. " +
                                "Add 'speedy-jooq-query-processor' dependency or override queryBackend() in ISpeedyConfiguration."))
                .create(dataSource, dialect, context);
    }

    default List<SpeedyTypeModule> typeModules() {
        return List.of();
    }

}
