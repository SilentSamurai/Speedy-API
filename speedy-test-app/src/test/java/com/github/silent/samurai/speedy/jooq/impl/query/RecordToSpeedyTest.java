package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.data.ComposedProduct;
import com.github.silent.samurai.speedy.data.Product;
import com.github.silent.samurai.speedy.data.ProductItem;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.backend.RowReader;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyInt;
import com.github.silent.samurai.speedy.models.SpeedyText;
import com.github.silent.samurai.speedy.walker.RecordToSpeedy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RecordToSpeedyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordToSpeedyTest.class);

    @Mock
    RowReader rowReader;

    RecordToSpeedy recordToSpeedy;

    @BeforeEach
    void setUp() {
        recordToSpeedy = new RecordToSpeedy(rowReader);
    }

    @Test
    void fromRecord() throws SpeedyHttpException {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);

        SpeedyEntity row = new SpeedyEntity(entityMetadata);
        row.put(entityMetadata.field("name"), new SpeedyText("Product 1"));
        row.put(entityMetadata.field("id"), new SpeedyText("1"));
        row.put(entityMetadata.field("cost"), new SpeedyInt(100L));
        row.put(entityMetadata.field("category"), new SpeedyText("cat-2"));

        SpeedyEntity speedyEntity = recordToSpeedy.fromRow(row, entityMetadata, Set.of());
        LOGGER.info("speedyEntity: {}", speedyEntity);

        assertNotNull(speedyEntity);

        assertTrue(speedyEntity.get(entityMetadata.field("name")).isText());
        assertEquals("Product 1", speedyEntity.get(entityMetadata.field("name")).asText());

        assertTrue(speedyEntity.get(entityMetadata.field("id")).isText());
        assertEquals("1", speedyEntity.get(entityMetadata.field("id")).asText());

        assertTrue(speedyEntity.get(entityMetadata.field("cost")).isNumber());
        assertEquals(100, speedyEntity.get(entityMetadata.field("cost")).asInt());

        assertTrue(speedyEntity.get(entityMetadata.field("category")).isText());
        assertEquals("cat-2", speedyEntity.get(entityMetadata.field("category")).asText());
    }

    @Test
    void fromRecordWithFK() throws SpeedyHttpException {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(ComposedProduct.class);
        EntityMetadata productItemMetadata = StaticEntityMetadata.createEntityMetadata(ProductItem.class);

        SpeedyEntity row = new SpeedyEntity(entityMetadata);
        row.put(entityMetadata.field("name"), new SpeedyText("Product 1"));
        row.put(entityMetadata.field("id"), new SpeedyText("1"));
        row.put(entityMetadata.field("category"), new SpeedyText("cat-2"));
        // foreign key stored under the association field
        row.put(entityMetadata.field("productItem"), new SpeedyText("1"));

        SpeedyEntity speedyEntity = recordToSpeedy.fromRow(row, entityMetadata, Set.of());
        LOGGER.info("speedyEntity: {}", speedyEntity);

        assertNotNull(speedyEntity);

        assertTrue(speedyEntity.get(entityMetadata.field("name")).isText());
        assertEquals("Product 1", speedyEntity.get(entityMetadata.field("name")).asText());

        assertTrue(speedyEntity.get(entityMetadata.field("id")).isText());
        assertEquals("1", speedyEntity.get(entityMetadata.field("id")).asText());

        assertTrue(speedyEntity.get(entityMetadata.field("productItem")).isObject());
        SpeedyEntity productItem = speedyEntity.get(entityMetadata.field("productItem")).asObject();
        assertNotNull(productItem);
        assertTrue(productItem.get(productItemMetadata.field("id")).isText());
        assertEquals("1", productItem.get(productItemMetadata.field("id")).asText());

        assertTrue(speedyEntity.get(entityMetadata.field("category")).isText());
        assertEquals("cat-2", speedyEntity.get(entityMetadata.field("category")).asText());
    }

    @Test
    void createSpeedyKeyFromFK() throws SpeedyHttpException {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(ComposedProduct.class);

        SpeedyEntity row = new SpeedyEntity(entityMetadata);
        row.put(entityMetadata.field("productItem"), new SpeedyText("1"));

        FieldMetadata productItem = entityMetadata.field("productItem");

        Optional<SpeedyEntityKey> optional = recordToSpeedy.createSpeedyKeyFromFK(row, productItem);
        SpeedyEntityKey speedyEntityKey = optional.get();

        LOGGER.info("speedyKeyFromFK: {}", optional);

        assertTrue(speedyEntityKey.get(entityMetadata.field("id")).isText());
        assertEquals("1", speedyEntityKey.get(entityMetadata.field("id")).asText());

    }
}
