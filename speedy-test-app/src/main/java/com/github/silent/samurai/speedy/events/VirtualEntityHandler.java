package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.entity.Product;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyVirtualEntityHandler;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.repositories.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VirtualEntityHandler implements SpeedyVirtualEntityHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualEntityHandler.class);

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    private Category getVirtualCategory() {
        Optional<Category> optional = categoryRepository.findByName("Virtual");
        return optional.orElseGet(() -> {
            Category newCategory = new Category();
            newCategory.setName("Virtual");
            return categoryRepository.save(newCategory);
        });
    }

    private void saveProduct(Product product) {
        product.setCategory(getVirtualCategory());
        productRepository.save(product);
    }

    @Override
    public SpeedyEntity create(SpeedyEntity entity) throws NotFoundException {
        LOGGER.info("VirtualEntity create Event");
        EntityMetadata entityMetadata = entity.getEntityMetadata();
        FieldMetadata id = entityMetadata.field("id");
        FieldMetadata name = entityMetadata.field("name");
        FieldMetadata description = entityMetadata.field("description");
        Product product = new Product();
        product.setName(entity.get(name).asText());
        product.setDescription(entity.get(description).asText());
        this.saveProduct(product);
        entity.put(id, SpeedyValueFactory.fromText(product.getId()));
        return entity;
    }

    @Override
    public SpeedyEntity update(SpeedyEntityKey pk, SpeedyEntity entity) throws NotFoundException {
        EntityMetadata entityMetadata = entity.getEntityMetadata();
        FieldMetadata id = entityMetadata.field("id");
        FieldMetadata description = entityMetadata.field("description");
        String idValue = pk.get(id).asText();
        Optional<Product> byId = productRepository.findById(idValue);
        Product product = byId.get();
        product.setDescription(entity.get(description).asText());
        this.saveProduct(product);
        entity.put(id, SpeedyValueFactory.fromText(product.getId()));
        return entity;
    }

    @Override
    public SpeedyEntity delete(SpeedyEntityKey pk) throws NotFoundException {
        EntityMetadata entityMetadata = pk.getEntityMetadata();
        FieldMetadata id = entityMetadata.field("id");
        String idValue = pk.get(id).asText();
        productRepository.deleteById(idValue);
        return pk;
    }

}
