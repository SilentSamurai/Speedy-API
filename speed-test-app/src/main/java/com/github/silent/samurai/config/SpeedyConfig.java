package com.github.silent.samurai.config;

import com.github.silent.samurai.JpaMetaModelProcessor;
import com.github.silent.samurai.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.interfaces.ISpeedyCustomValidation;
import com.github.silent.samurai.interfaces.ISpeedyOpenApiConfiguration;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.validation.SpeedyValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

@Configuration
public class SpeedyConfig implements ISpeedyConfiguration, ISpeedyOpenApiConfiguration {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyValidation speedyValidation;

    @Override
    public EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    @Override
    public MetaModelProcessor createMetaModelProcessor() {
        return new JpaMetaModelProcessor(entityManagerFactory);
    }

    @Override
    public ISpeedyCustomValidation getCustomValidator() {
        return speedyValidation;
    }
}
