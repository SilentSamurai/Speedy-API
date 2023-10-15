package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.entity.Product;
import com.github.silent.samurai.speedy.entity.VirtualEntity;
import com.github.silent.samurai.speedy.interfaces.SpeedyVirtualEntityHandler;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.repositories.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VirtualEntityHandler implements SpeedyVirtualEntityHandler<VirtualEntity> {

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

    private void saveProduct(VirtualEntity entity, Product product) {
        product.setDescription(entity.getDescription());
        product.setName(entity.getName());
        product.setCategory(getVirtualCategory());
        productRepository.save(product);
        entity.setId(product.getId());
    }

    @Override
    public VirtualEntity create(VirtualEntity entity) {
        LOGGER.info("VirtualEntity create Event");
        Product product = new Product();
        this.saveProduct(entity, product);
        return entity;
    }

    @Override
    public VirtualEntity update(VirtualEntity entity) {
        Optional<Product> byId = productRepository.findById(entity.getId());
        this.saveProduct(entity, byId.get());
        return entity;
    }

    @Override
    public VirtualEntity delete(VirtualEntity entity) {
        productRepository.deleteById(entity.getId());
        return entity;
    }

}
