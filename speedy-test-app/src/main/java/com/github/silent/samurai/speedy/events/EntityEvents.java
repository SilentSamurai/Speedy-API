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

//    @SpeedyEvent(value = Category.class, eventType = SpeedyEventType.PRE_INSERT)
//    public void categoryInsertEvent(Category category) throws Exception {
//        LOGGER.info("Category Pre Insert Event");
//    }

    @SpeedyEvent(value = Category.class, eventType = {SpeedyEventType.POST_INSERT, SpeedyEventType.PRE_INSERT})
    public void categoryPostInsertEvent(Category category) throws Exception {
        LOGGER.info("Category Post Insert Event");
    }
}
