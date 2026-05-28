package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;

import javax.sql.DataSource;

public interface ISpeedyConfiguration {

    MetaModelProcessor metaModelProcessor();

    void register(ISpeedyRegistry registry);

    DataSource dataSourcePerReq();

    SpeedyDialect getDialect();

    default boolean isMetadataEndpointEnabled() {
        return true;
    }

    default int getMaxPageSize() {
        return 1000;
    }

    default long getMaxRequestBodySize() {
        return 1_048_576;
    }

}
