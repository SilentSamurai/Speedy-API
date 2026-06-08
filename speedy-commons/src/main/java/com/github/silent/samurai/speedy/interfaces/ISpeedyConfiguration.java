package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessorProvider;

import javax.sql.DataSource;
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

    default long getMaxRequestBodySize() {
        return 1_048_576;
    }

    default QueryProcessor queryProcessor(DataSource dataSource, SpeedyDialect dialect) {
        return ServiceLoader.load(QueryProcessorProvider.class)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(
                        "No QueryProcessor implementation on classpath. " +
                                "Add 'speedy-jooq-query-processor' dependency or override queryProcessor() in ISpeedyConfiguration."))
                .create(dataSource, dialect);
    }

}
