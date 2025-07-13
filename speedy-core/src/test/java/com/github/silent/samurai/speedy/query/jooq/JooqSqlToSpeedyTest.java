package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.data.ComposedProduct;
import com.github.silent.samurai.speedy.data.Product;
import com.github.silent.samurai.speedy.data.ProductItem;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JooqSqlToSpeedyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqSqlToSpeedyTest.class);

    @Mock
    DSLContext dslContext;

    @Mock
    Record record;

    JooqSqlToSpeedy jooqSqlToSpeedy;

    @BeforeEach
    void setUp() {
        Mockito.when(dslContext.dialect()).thenReturn(SQLDialect.H2);
        jooqSqlToSpeedy = new JooqSqlToSpeedy(dslContext);
    }

    private <T> OngoingStubbing<T> mockRecord(EntityMetadata entityMetadata, String fieldName) throws NotFoundException {
        FieldMetadata fieldMetadata = entityMetadata.field(fieldName);
        Field<T> column = JooqUtil.getColumn(fieldMetadata, SQLDialect.H2);
        return Mockito.when(
                record.get(column, column.getType())
        );
    }

    @Test
    void fromRecord() throws SpeedyHttpException {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);

        mockRecord(entityMetadata, "name").thenReturn("Product 1");
        mockRecord(entityMetadata, "id").thenReturn("1");
        mockRecord(entityMetadata, "cost").thenReturn(100);
        mockRecord(entityMetadata, "category").thenReturn("cat-2");

        SpeedyEntity speedyEntity = jooqSqlToSpeedy.fromRecord(record, entityMetadata, List.of());
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


        mockRecord(entityMetadata, "name").thenReturn("Product 1");
        mockRecord(entityMetadata, "id").thenReturn("1");
        mockRecord(entityMetadata, "category").thenReturn("cat-2");

        mockRecord(entityMetadata, "productItem").thenReturn("1");

//        Mockito.when(record.getValue("NAME")).thenReturn("Product 1");
//        Mockito.when(record.getValue("ID")).thenReturn("1");
//        Mockito.when(record.getValue("CATEGORY")).thenReturn("cat-2");
//        Mockito.when(record.getValue("PRODUCTITEM")).thenReturn("1");

        SpeedyEntity speedyEntity = jooqSqlToSpeedy.fromRecord(record, entityMetadata, List.of());
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

        mockRecord(entityMetadata, "productItem").thenReturn("1");

        FieldMetadata productItem = entityMetadata.field("productItem");

        Optional<SpeedyEntityKey> optional = jooqSqlToSpeedy.createSpeedyKeyFromFK(record, productItem);
        SpeedyEntityKey speedyEntityKey = optional.get();

        LOGGER.info("speedyKeyFromFK: {}", optional);

        assertTrue(speedyEntityKey.get(entityMetadata.field("id")).isText());
        assertEquals("1", speedyEntityKey.get(entityMetadata.field("id")).asText());

    }
}