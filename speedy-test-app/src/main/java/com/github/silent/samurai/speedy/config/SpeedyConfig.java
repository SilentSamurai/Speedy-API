package com.github.silent.samurai.speedy.config;

import com.github.silent.samurai.speedy.events.EntityEvents;
import com.github.silent.samurai.speedy.events.VirtualEntityHandler;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.ISpeedyCustomValidation;
import com.github.silent.samurai.speedy.interfaces.ISpeedyRegistry;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.processors.JpaMetaModelProcessor;
import com.github.silent.samurai.speedy.validation.SpeedyValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

@Configuration
public class SpeedyConfig implements ISpeedyConfiguration {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyValidation speedyValidation;

    @Autowired
    EntityEvents entityEvents;

    @Autowired
    VirtualEntityHandler virtualEntityHandler;

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

    @Override
    public void register(ISpeedyRegistry registry) {
        registry.registerEventHandler(entityEvents);
        registry.registerVirtualEntityHandler(virtualEntityHandler);
    }
}
