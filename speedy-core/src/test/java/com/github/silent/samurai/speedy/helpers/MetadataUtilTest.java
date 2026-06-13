package com.github.silent.samurai.speedy.helpers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.data.*;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.persistence.EntityManager;
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
class MetadataUtilTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataUtilTest.class);

    @Mock
    EntityManager entityManager;

    @Mock
    MetaModel metaModel;

    @BeforeEach
    void setUp() throws NotFoundException {

    }

    @Test
    void isPrimaryKeyComplete() {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        Set<String> fields = Set.of("id");
        ObjectNode objectNode = CommonUtil.json().createObjectNode();
        objectNode.put("id", "pol-pol-ois");
        assertTrue(MetadataUtil.isPrimaryKeyComplete(entityMetadata, objectNode));
    }

    @Test
    void isNullPrimaryKeyComplete() {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        Set<String> fields = Set.of("id");
        ObjectNode objectNode = CommonUtil.json().createObjectNode();
        objectNode.putNull("id");
        assertFalse(MetadataUtil.isPrimaryKeyComplete(entityMetadata, objectNode));
    }

    @Test
    void hasOnlyPrimaryKeyFields() {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        Set<String> fields = Set.of("id");
        assertTrue(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields1() {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        Set<String> fields = Set.of("id", "name");
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields2() {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        Set<String> fields = Set.of();
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields2_1() {
        EntityMetadata entityMetadata = Mockito.mock(EntityMetadata.class);
//        Mockito.when(entityMetadata.getKeyFields()).thenReturn(Sets.newHashSet());
        Set<String> fields = Set.of();
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }


    @Test
    void hasOnlyPrimaryKeyFields3() {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
        Set<String> fields = Set.of("id");
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields4() {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
        Set<String> fields = Set.of("id", "name", "category");
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

//    @Test
//    void createEntityKeyFromMap() throws Exception {
//        EntityMetadata productMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
//        Mockito.when(metaModel.findEntityMetadata(Mockito.anyString())).thenReturn(productMetadata);
//        SpeedyUriContext speedyUriContext = new SpeedyUriContext(metaModel, "/Category?id='1234'");
//        SpeedyQuery speedyQuery = speedyUriContext.parse();
//        SpeedyEntityKey primaryKey = MetadataUtil.createIdentifierFromQuery(speedyQuery);
//        FieldMetadata id = productMetadata.field("id");
//        assertEquals("1234", primaryKey.get(id).asText());
//    }
//
//    @Test
//    void createEntityKeyFromMap1() throws Exception {
//        EntityMetadata productMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
//        Mockito.when(metaModel.findEntityMetadata(Mockito.anyString())).thenReturn(productMetadata);
//        SpeedyUriContext speedyUriContext = new SpeedyUriContext(metaModel, "/Category?id='1234'&name='na'");
//        SpeedyQuery speedyQuery = speedyUriContext.parse();
//
//        SpeedyEntityKey primaryKey = MetadataUtil.createIdentifierFromQuery(speedyQuery);
//        FieldMetadata id = productMetadata.field("id");
//        FieldMetadata name = productMetadata.field("name");
//        assertEquals("1234", primaryKey.get(id).asText());
//        assertEquals("na", primaryKey.get(name).asText());
//    }
//
//    @Test
//    void createEntityKeyFromMap2() throws Exception {
//        BadRequestException badRequestException = assertThrows(BadRequestException.class,
//                () -> {
//                    EntityMetadata productMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
//                    Mockito.when(metaModel.findEntityMetadata(Mockito.anyString())).thenReturn(productMetadata);
//                    SpeedyUriContext speedyUriContext = new SpeedyUriContext(metaModel, "/Category(name='1234')");
//                    SpeedyQuery speedyQuery = speedyUriContext.parse();
//                    MetadataUtil.createIdentifierFromQuery(speedyQuery);
//                }
//        );
//    }
//
//    @Test
//    void createEntityKeyFromMap3() throws Exception {
//        BadRequestException badRequestException = assertThrows(BadRequestException.class,
//                () -> {
//                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
//                    Mockito.when(metaModel.findEntityMetadata(Mockito.anyString())).thenReturn(entityMetadata);
//                    SpeedyUriContext speedyUriContext = new SpeedyUriContext(metaModel, "/Category(name='na')");
//                    SpeedyQuery speedyQuery = speedyUriContext.parse();
//                    MetadataUtil.createIdentifierFromQuery(speedyQuery);
//                }
//        );
//    }


}