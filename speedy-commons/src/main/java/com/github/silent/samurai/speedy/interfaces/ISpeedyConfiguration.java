package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;

import javax.sql.DataSource;

public interface ISpeedyConfiguration {

    MetaModelProcessor metaModelProcessor();

    void register(ISpeedyRegistry registry);

    DataSource getDataSource();

    SpeedyDialect getDialect();

}
