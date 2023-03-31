package com.github.silent.samurai.interfaces;

import javax.persistence.EntityManager;

public interface ISpeedyConfiguration {

    EntityManager createEntityManager();

    MetaModelProcessor createMetaModelProcessor();

}
