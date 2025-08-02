package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.annotations.SpeedyEvent;
import com.github.silent.samurai.speedy.entity.User;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.interfaces.ISpeedyEventHandler;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.utils.Speedy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EntityEvents implements ISpeedyEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityEvents.class);

    @SpeedyEvent(value = "Category", eventType = {SpeedyEventType.POST_INSERT, SpeedyEventType.PRE_INSERT})
    public void categoryPostInsertEvent(SpeedyEntity category) throws Exception {
        LOGGER.info("Category Post Insert Event");
    }

    @SpeedyEvent(value = "User", eventType = {SpeedyEventType.PRE_INSERT})
    public void userInsert(User user) throws Exception {
        LOGGER.info("User Insert Event");
        user.setCreatedAt(LocalDateTime.now());
    }

    @SpeedyEvent(value = "User", eventType = {SpeedyEventType.PRE_UPDATE})
    public void userUpdate(User user) throws Exception {
        LOGGER.info("User Update Event");
        user.setUpdatedAt(LocalDateTime.now());
    }

    @SpeedyEvent(value = "User", eventType = {SpeedyEventType.PRE_DELETE})
    public void userDelete(User user) throws Exception {
        LOGGER.info("User Delete Event");
        user.setDeletedAt(LocalDateTime.now());
    }
}
