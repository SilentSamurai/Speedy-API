package com.github.silent.samurai.speedy.interfaces;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

public interface ISpeedyConfiguration {

    MetaModelProcessor createMetaModelProcessor();

    void register(ISpeedyRegistry registry);

    DataSource getDataSource();

    String getDialect();

}
