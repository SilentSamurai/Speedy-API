# Setting up Virtual Entity

An Entity on view, with custom create, edit & delete operations used for UI generation or if tables have complex
relations.

### Overview

- create a view for all get operations.
- create custom handler for create, edit & delete.

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
    public void register(ISpeedyRegistry registry) {
        // register custom handler for entity 
        registry.registerVirtualEntityHandler(virtualEntityHandler);
    }
}


```

### Create Virtual Entity Class on View

```java
package com.github.silent.samurai.speedy.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@Table(name = "PRODUCT_VIEW")
@Entity
public class VirtualEntity {

    protected static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    protected String id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", length = 1024)
    private String description;

}

```

### Create Handler for create, edit & delete actions

```java
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

```