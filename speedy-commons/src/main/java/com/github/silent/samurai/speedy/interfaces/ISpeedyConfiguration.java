package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;

import javax.sql.DataSource;

public interface ISpeedyConfiguration {

    MetaModel createMetaModelProcessor();

    void register(ISpeedyRegistry registry);

    DataSource getDataSource();

    SpeedyDialect getDialect();

}
