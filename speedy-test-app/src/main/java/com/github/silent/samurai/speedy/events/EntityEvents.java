package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.annotations.SpeedyEvent;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.interfaces.ISpeedyEventHandler;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EntityEvents implements ISpeedyEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityEvents.class);

    @SpeedyEvent(value = "Category", eventType = {SpeedyEventType.POST_INSERT, SpeedyEventType.PRE_INSERT})
    public void categoryPostInsertEvent(SpeedyEntity category) throws Exception {
        LOGGER.info("Category Post Insert Event");
    }
}
