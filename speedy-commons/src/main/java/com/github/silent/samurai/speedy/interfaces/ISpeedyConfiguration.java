package com.github.silent.samurai.speedy.interfaces;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public interface ISpeedyConfiguration {

    EntityManager createEntityManager();

    EntityManagerFactory createEntityManagerFactory();

    MetaModelProcessor createMetaModelProcessor();

    ISpeedyCustomValidation getCustomValidator();

    void register(ISpeedyRegistry registry);

}
