package com.github.silent.samurai.speedy.interfaces;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

public interface ISpeedyConfiguration {

    MetaModelProcessor createMetaModelProcessor();

    ISpeedyCustomValidation getCustomValidator();

    void register(ISpeedyRegistry registry);

    DataSource getDataSource();

    String getDialect();

}
