package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.annotations.SpeedyEvent;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.entity.Company;
import com.github.silent.samurai.speedy.entity.CompanyStatus;
import com.github.silent.samurai.speedy.entity.User;
import com.github.silent.samurai.speedy.entity.Product;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.interfaces.ISpeedyEventHandler;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.utils.Speedy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EntityEvents implements ISpeedyEventHandler {

    @Autowired
    private CategoryRepository categoryRepository;

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

    @SpeedyEvent(value = "Product", eventType = {SpeedyEventType.PRE_INSERT})
    public void productInsert(Product product) throws Exception {
        LOGGER.info("Product Insert Event");
        // mark description so tests can verify handler execution (always override on insert)
        product.setDescription("created-by-event");
        if (product.getCategory() != null && product.getCategory().getId() != null && !product.getCategory().getId().isBlank()) {
            Category category = categoryRepository.findById(product.getCategory().getId()).orElse(null);
            product.setCategory(category);
        }
    }

    @SpeedyEvent(value = "Product", eventType = {SpeedyEventType.PRE_UPDATE})
    public void productUpdate(Product product) throws Exception {
        LOGGER.info("Product Update Event");
        // mark description so tests can verify handler execution
        product.setDescription("updated-by-event");
        if (product.getCategory() != null && product.getCategory().getId() != null && !product.getCategory().getId().isBlank()) {
            Category category = categoryRepository.findById(product.getCategory().getId()).orElse(null);
            product.setCategory(category);
        }

    }

    @SpeedyEvent(value = "Company", eventType = {SpeedyEventType.PRE_INSERT})
    public void companyInsert(Company company) throws Exception {
        LOGGER.info("Company Insert Event");
        if (company.getStatus() == null) {
            company.setStatus(CompanyStatus.DRAFT);
        }
        company.setCreatedAt(LocalDateTime.now());
    }

    @SpeedyEvent(value = "Company", eventType = {SpeedyEventType.PRE_UPDATE})
    public void companyUpdate(Company company) throws Exception {
        LOGGER.info("Company Update Event");
        company.setUpdatedAt(LocalDateTime.now());
    }

    @SpeedyEvent(value = "Company", eventType = {SpeedyEventType.PRE_DELETE})
    public void companyDelete(Company company) throws Exception {
        LOGGER.info("Company Delete Event");
        company.setDeletedAt(LocalDateTime.now());
    }
}
