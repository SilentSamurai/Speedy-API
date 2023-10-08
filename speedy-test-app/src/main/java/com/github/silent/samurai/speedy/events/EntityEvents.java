package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.annotations.SpeedyEvent;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.entity.Product;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.interfaces.ISpeedyEventHandler;
import com.github.silent.samurai.speedy.repositories.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EntityEvents implements ISpeedyEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityEvents.class);

    @Autowired
    ProductRepository productRepository;

    @SpeedyEvent(value = Product.class, eventType = {SpeedyEventType.IN_PLACE_OF_INSERT, SpeedyEventType.IN_PLACE_OF_UPDATE})
    public Product productInsertEvent(Product product) throws Exception {
        LOGGER.info("product in place Event");
        product.setDescription("in-place description");
        return productRepository.save(product);
    }

    @SpeedyEvent(value = Category.class, eventType = {SpeedyEventType.POST_INSERT, SpeedyEventType.PRE_INSERT})
    public void categoryPostInsertEvent(Category category) throws Exception {
        LOGGER.info("Category Post Insert Event");
    }
}
