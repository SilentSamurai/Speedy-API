package com.github.silent.samurai.speedy.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.data.*;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MetadataUtilTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataUtilTest.class);

    @Mock
    EntityManager entityManager;

    @Mock
    MetaModelProcessor metaModelProcessor;

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

    @Test
    void createEntityKeyFromMap() throws Exception {
        EntityMetadata productMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(productMetadata);
        SpeedyUriContext speedyUriContext = new SpeedyUriContext(metaModelProcessor, "/Category(id='1234')");
        SpeedyQuery speedyQuery = speedyUriContext.parse();
        SpeedyEntityKey primaryKey = MetadataUtil.createIdentifierFromQuery(speedyQuery);
        FieldMetadata id = productMetadata.field("id");
        assertEquals("1234", primaryKey.get(id).asText());
    }

    @Test
    void createEntityKeyFromMap1() throws Exception {
        EntityMetadata productMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
        Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(productMetadata);
        SpeedyUriContext speedyUriContext = new SpeedyUriContext(metaModelProcessor, "/Category(id='1234', name='na')");
        SpeedyQuery speedyQuery = speedyUriContext.parse();

        SpeedyEntityKey primaryKey = MetadataUtil.createIdentifierFromQuery(speedyQuery);
        FieldMetadata id = productMetadata.field("id");
        FieldMetadata name = productMetadata.field("name");
        assertEquals("1234", primaryKey.get(id).asText());
        assertEquals("na", primaryKey.get(name).asText());
    }

    @Test
    void createEntityKeyFromMap2() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata productMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
                    Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(productMetadata);
                    SpeedyUriContext speedyUriContext = new SpeedyUriContext(metaModelProcessor, "/Category(name='1234')");
                    SpeedyQuery speedyQuery = speedyUriContext.parse();
                    MetadataUtil.createIdentifierFromQuery(speedyQuery);
                }
        );
    }

    @Test
    void createEntityKeyFromMap3() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
                    Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(entityMetadata);
                    SpeedyUriContext speedyUriContext = new SpeedyUriContext(metaModelProcessor, "/Category(name='na')");
                    SpeedyQuery speedyQuery = speedyUriContext.parse();
                    MetadataUtil.createIdentifierFromQuery(speedyQuery);
                }
        );
    }


    @Test
    void createEntityKeyFromJSON() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'1234', 'name':'na'}");
        SpeedyEntityKey primaryKey = MetadataUtil.createIdentifierFromJSON(entityMetadata, (ObjectNode) jsonElement);
        assertEquals("1234", primaryKey.get(entityMetadata.field("id")).asText());
    }

    @Test
    void createEntityKeyFromJSON1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'1234', 'name':'na'}");
        SpeedyEntityKey primaryKey = MetadataUtil.createIdentifierFromJSON(entityMetadata, (ObjectNode) jsonElement);
        assertEquals("1234", primaryKey.get(entityMetadata.field("id")).asText());
        assertEquals("na", primaryKey.get(entityMetadata.field("name")).asText());
    }

    @Test
    void createEntityKeyFromJSON2() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
                    JsonNode jsonElement = CommonUtil.json().readTree("{'name':'na'}");
                    MetadataUtil.createIdentifierFromJSON(entityMetadata, (ObjectNode) jsonElement);
                }
        );
    }

    @Test
    void createEntityKeyFromJSON3() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
                    JsonNode jsonElement = CommonUtil.json().readTree("{'name':'na'}");
                    MetadataUtil.createIdentifierFromJSON(entityMetadata, (ObjectNode) jsonElement);
                }
        );
    }

    /*@Test
    void createEntityObjectFromMap() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
        HashMap hashMap = CommonUtil.getGson().fromJson("{'id':'abcd', 'name':'na', 'category':'cat-1'}", HashMap.class);
        EntityTestClass entity = (EntityTestClass) MetadataUtil.createEntityFromMap(entityMetadata, hashMap, entityManager);
        assertEquals("abcd", entity.getId());
        assertEquals("na", entity.getName());
        assertEquals("cat-1", entity.getCategory());
    }

    @Test
    void createEntityObjectFromMap1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        HashMap hashMap = CommonUtil.getGson().fromJson("{'id':'abcd', 'name':'na', 'category':'cat-1'}", HashMap.class);
        EntityCompositeKeyTestClass entity = (EntityCompositeKeyTestClass) MetadataUtil.createEntityFromMap(entityMetadata, hashMap, entityManager);
        assertEquals("abcd", entity.getId());
        assertEquals("na", entity.getName());
        assertEquals("cat-1", entity.getCategory());
    }*/

    @Test
    void createEntityObjectFromJSON() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'abcd', 'name':'na', 'category':'cat-1'}");
        SpeedyEntity entity = MetadataUtil.createEntityFromJSON(entityMetadata, (ObjectNode) jsonElement);
        assertEquals("abcd", entity.get(entityMetadata.field("id")).asText());
        assertEquals("na", entity.get(entityMetadata.field("name")).asText());
        assertEquals("cat-1", entity.get(entityMetadata.field("category")).asText());
    }

    @Test
    void createEntityObjectFromJSON1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'abcd', 'name':'na', 'category':'cat-1'}");
        SpeedyEntity entity = MetadataUtil.createEntityFromJSON(entityMetadata, (ObjectNode) jsonElement);
        assertEquals("abcd", entity.get(entityMetadata.field("id")).asText());
        assertEquals("na", entity.get(entityMetadata.field("name")).asText());
        assertEquals("cat-1", entity.get(entityMetadata.field("category")).asText());
    }

    /*@Test
    void updateEntityFromJson1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        HashMap hashMap = CommonUtil.getGson().fromJson("{'id':'abcd', 'name':'na', 'category':'cat-1'}", HashMap.class);
        EntityCompositeKeyTestClass entity = (EntityCompositeKeyTestClass) MetadataUtil.createEntityFromMap(entityMetadata, hashMap, entityManager);
        assertEquals("abcd", entity.getId());
        assertEquals("na", entity.getName());
        assertEquals("cat-1", entity.getCategory());

        JsonElement jsonElement = CommonUtil.getGson().fromJson("{'category':'cat-2'}", JsonElement.class);

        MetadataUtil.updateEntityFromJSON(entityMetadata, entityManager, (ObjectNode) jsonElement, entity);
        assertEquals("cat-2", entity.getCategory());
    }*/

    @Test
    void createEntityFromJson2() throws Exception {
        ProductItem productItem = new ProductItem();
        productItem.setId("abcd");
        productItem.setName("Part - 1");
        EntityMetadata productMetadata = StaticEntityMetadata.createEntityMetadata(ComposedProduct.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'abcd', 'name':'na', 'category':'cat-1', 'productItem':{'id':'abcd'} }");
        SpeedyEntity productEntity = MetadataUtil.createEntityFromJSON(productMetadata, (ObjectNode) jsonElement);
        assertEquals("abcd", productEntity.get(productMetadata.field("id")).asText());
        assertEquals("na", productEntity.get(productMetadata.field("name")).asText());
        assertEquals("cat-1", productEntity.get(productMetadata.field("category")).asText());
        FieldMetadata fieldMetadata = productMetadata.field("productItem");
        SpeedyEntity productItemEntity = productEntity.get(fieldMetadata).asObject();
        FieldMetadata id = fieldMetadata.getAssociationMetadata().field("id");
        assertEquals(productItem.getId(), productItemEntity.get(id).asText());
    }

    @Test
    void createEntityFromJsonNegativeTest() throws Exception {
        EntityMetadata productMetadata = StaticEntityMetadata.createEntityMetadata(ComposedProduct.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'abcd', 'name':'na', 'category':'cat-1', 'productItem': null }");
        SpeedyEntity productEntity = MetadataUtil.createEntityFromJSON(productMetadata, (ObjectNode) jsonElement);
        // assert productEntity is not null
        assertNotNull(productEntity);
        // assert id is not null
        SpeedyValue id = productEntity.get(productMetadata.field("id"));
        assertNotNull(id);
        // assert id is abcd
        assertEquals("abcd", id.asText());
        // assert name is not null
        SpeedyValue name = productEntity.get(productMetadata.field("name"));
        assertNotNull(name);
        // assert name is na
        assertEquals("na", name.asText());
        // assert productItem is null
        assertFalse(productEntity.has(productMetadata.field("productItem")));
    }
}