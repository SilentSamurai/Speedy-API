package com.github.silent.samurai.speedy.jpa.impl.util;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.jpa.impl.data.ComposedProduct;
import com.github.silent.samurai.speedy.jpa.impl.data.Product;
import com.github.silent.samurai.speedy.jpa.impl.data.ProductItem;
import com.github.silent.samurai.speedy.jpa.impl.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommonUtilTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonUtilTest.class);

    @Mock
    MetaModelProcessor metaModelProcessor;

    @Mock
    EntityManager entityManager;


    @BeforeEach
    void setUp() throws NotFoundException {

    }

    @Test
    void combine() throws Exception {

        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        Mockito.when(metaModelProcessor.findEntityMetadata("Product")).thenReturn(entityMetadata);

        Product product = new Product();
        product.setId("1");
        product.setName("abcd");
        product.setCost(0L);
        product.setCategory("cat-1");

        SpeedyEntity entity = CommonUtil.fromJpaEntity(product, entityMetadata, Collections.emptySet());

        LOGGER.info("Speedy Entity: {}", entity);

        Product parsedProduct = new Product();

        CommonUtil.updateFromSpeedyEntity(entity, parsedProduct, entityMetadata, entityManager);

        LOGGER.info("Speedy Entity: {}", parsedProduct);

        Assertions.assertEquals(parsedProduct, product);
    }

    @Test
    void combine1() throws Exception {

        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(ComposedProduct.class);

        ProductItem productItem = new ProductItem();
        productItem.setName("Product Item");
        productItem.setId("1");

        Mockito.when(entityManager.find(Mockito.any(), Mockito.any())).thenReturn(productItem);

        ComposedProduct product = new ComposedProduct();
        product.setId("1");
        product.setName("abcd");
        product.setProductItem(productItem);
        product.setCategory("cat-1");

        SpeedyEntity entity = CommonUtil.fromJpaEntity(product, entityMetadata, Sets.newSet("ProductItem"));

        LOGGER.info("Speedy Entity: {}", entity);

        ComposedProduct parsedProduct = new ComposedProduct();

        CommonUtil.updateFromSpeedyEntity(entity, parsedProduct, entityMetadata, entityManager);

        LOGGER.info("Speedy Entity: {}", parsedProduct);


        Assertions.assertEquals(parsedProduct.getId(), product.getId());
        Assertions.assertEquals(parsedProduct.getName(), product.getName());
        Assertions.assertEquals(parsedProduct.getCategory(), product.getCategory());
        Assertions.assertNotNull(parsedProduct.getProductItem());
        Assertions.assertEquals(parsedProduct.getProductItem().getId(), product.getProductItem().getId());
        Assertions.assertEquals(parsedProduct.getProductItem().getName(), product.getProductItem().getName());
    }
}