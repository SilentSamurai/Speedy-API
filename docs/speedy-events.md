# Speedy Events

### Overview

attach events to be fired before different actions Insert, Update & Delete.

#### Events present

- PRE_INSERT
- PRE_UPDATE
- PRE_DELETE
- POST_INSERT
- POST_UPDATE
- POST_DELETE

### Configuration

```java
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
    EntityEvents entityEvents;

    @Override
    public EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    @Override
    public MetaModelProcessor createMetaModelProcessor() {
        return new JpaMetaModelProcessor(entityManagerFactory);
    }

    @Override
    public void register(ISpeedyRegistry registry) {
        registry.registerEventHandler(entityEvents);
    }
}

```

### Event Handlers

```java
package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.annotations.SpeedyEvent;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.interfaces.ISpeedyEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EntityEvents implements ISpeedyEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityEvents.class);

    @SpeedyEvent(value = Category.class, eventType = {SpeedyEventType.POST_INSERT, SpeedyEventType.PRE_INSERT})
    public void categoryPostInsertEvent(Category category) throws Exception {
        LOGGER.info("Category Post Insert Event");
    }
}

```