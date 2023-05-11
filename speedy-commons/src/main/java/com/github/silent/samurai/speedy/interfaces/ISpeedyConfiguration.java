package com.github.silent.samurai.speedy.interfaces;

import javax.persistence.EntityManager;

public interface ISpeedyConfiguration {

    EntityManager createEntityManager();

    MetaModelProcessor createMetaModelProcessor();

    ISpeedyCustomValidation getCustomValidator();

}
