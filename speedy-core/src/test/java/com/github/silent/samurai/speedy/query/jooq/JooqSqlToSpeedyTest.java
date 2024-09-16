package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.data.Product;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JooqSqlToSpeedyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqSqlToSpeedyTest.class);

    @Mock
    DSLContext dslContext;

    @Mock
    Record record;

    JooqSqlToSpeedy jooqSqlToSpeedy;

    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);

    @BeforeEach
    void setUp() {
        jooqSqlToSpeedy = new JooqSqlToSpeedy(dslContext);
    }

    @Test
    void fromRecord() throws SpeedyHttpException {
        Mockito.when(record.getValue("NAME")).thenReturn("Product 1");
        Mockito.when(record.getValue("ID")).thenReturn("1");
        Mockito.when(record.getValue("COST")).thenReturn(100);
        Mockito.when(record.getValue("CATEGORY")).thenReturn("cat-2");

        SpeedyEntity speedyEntity = jooqSqlToSpeedy.fromRecord(record, entityMetadata, Set.of());
        LOGGER.info("speedyEntity: {}", speedyEntity);
        assertNotNull(speedyEntity);
        assertTrue(speedyEntity.get(entityMetadata.field("name")).isText());
        assertEquals(speedyEntity.get(entityMetadata.field("name")).asText(), "Product 1");
    }

    @Test
    void createSpeedyKeyFromFK() {
    }
}