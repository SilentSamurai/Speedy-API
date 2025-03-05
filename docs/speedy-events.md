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
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.processors.JpaMetaModelProcessor;
import com.github.silent.samurai.speedy.validation.SpeedyValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Configuration
public class SpeedyConfig implements ISpeedyConfiguration {

    
    @Autowired
    EntityEvents entityEvents;
    ...
    
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

    @SpeedyEvent(value = "User", eventType = {SpeedyEventType.PRE_INSERT})
    public void userInsert(SpeedyEntity user) throws Exception {
        LOGGER.info("User Insert Event");
        user.put("createdAt", Speedy.from(LocalDateTime.now()));
    }

    @SpeedyEvent(value = "User", eventType = {SpeedyEventType.PRE_UPDATE})
    public void userUpdate(SpeedyEntity user) throws Exception {
        LOGGER.info("User Update Event");
        user.put("updateAt", Speedy.from(LocalDateTime.now()));
    }

    @SpeedyEvent(value = "User", eventType = {SpeedyEventType.PRE_DELETE})
    public void userDelete(SpeedyEntity user) throws Exception {
        LOGGER.info("User Delete Event");
        user.put("deletedAt", Speedy.from(LocalDateTime.now()));
    }
}

```