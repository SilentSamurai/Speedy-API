package com.github.silent.samurai.speedy.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.data.*;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.json.registry.JsonRegistry;
import com.github.silent.samurai.speedy.json.walker.JsonToSpeedy;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/// JSON entity parsing tests moved from MetadataUtilTest.
/// MetadataUtil.createIdentifierFromJSON / createEntityFromJSON were removed
/// from speedy-core (they depended on JsonToSpeedy which lives here), so these
/// tests now call JsonToSpeedy directly.
@ExtendWith(MockitoExtension.class)
class JsonEntityParsingTest {

    private final JsonToSpeedy converter = new JsonToSpeedy(JsonRegistry.defaults());

    @Test
    void createEntityKeyFromJSON() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'1234', 'name':'na'}");
        SpeedyEntityKey primaryKey = converter.fromPkJson(entityMetadata, (ObjectNode) jsonElement);
        assertEquals("1234", primaryKey.get(entityMetadata.field("id")).asText());
    }

    @Test
    void createEntityKeyFromJSON1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'1234', 'name':'na'}");
        SpeedyEntityKey primaryKey = converter.fromPkJson(entityMetadata, (ObjectNode) jsonElement);
        assertEquals("1234", primaryKey.get(entityMetadata.field("id")).asText());
        assertEquals("na", primaryKey.get(entityMetadata.field("name")).asText());
    }

    @Test
    void createEntityKeyFromJSON2() throws Exception {
        assertThrows(BadRequestException.class, () -> {
            EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
            JsonNode jsonElement = CommonUtil.json().readTree("{'name':'na'}");
            converter.fromPkJson(entityMetadata, (ObjectNode) jsonElement);
        });
    }

    @Test
    void createEntityKeyFromJSON3() throws Exception {
        assertThrows(BadRequestException.class, () -> {
            EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
            JsonNode jsonElement = CommonUtil.json().readTree("{'name':'na'}");
            converter.fromPkJson(entityMetadata, (ObjectNode) jsonElement);
        });
    }

    @Test
    void createEntityObjectFromJSON() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'abcd', 'name':'na', 'category':'cat-1'}");
        SpeedyEntity entity = converter.fromEntityMetadata(entityMetadata, (ObjectNode) jsonElement);
        assertEquals("abcd", entity.get(entityMetadata.field("id")).asText());
        assertEquals("na", entity.get(entityMetadata.field("name")).asText());
        assertEquals("cat-1", entity.get(entityMetadata.field("category")).asText());
    }

    @Test
    void createEntityObjectFromJSON1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'abcd', 'name':'na', 'category':'cat-1'}");
        SpeedyEntity entity = converter.fromEntityMetadata(entityMetadata, (ObjectNode) jsonElement);
        assertEquals("abcd", entity.get(entityMetadata.field("id")).asText());
        assertEquals("na", entity.get(entityMetadata.field("name")).asText());
        assertEquals("cat-1", entity.get(entityMetadata.field("category")).asText());
    }

    @Test
    void createEntityFromJson2() throws Exception {
        ProductItem productItem = new ProductItem();
        productItem.setId("abcd");
        productItem.setName("Part - 1");
        EntityMetadata productMetadata = StaticEntityMetadata.createEntityMetadata(ComposedProduct.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'abcd', 'name':'na', 'category':'cat-1', 'productItem':{'id':'abcd'} }");
        SpeedyEntity productEntity = converter.fromEntityMetadata(productMetadata, (ObjectNode) jsonElement);
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
        SpeedyEntity productEntity = converter.fromEntityMetadata(productMetadata, (ObjectNode) jsonElement);
        assertNotNull(productEntity);
        SpeedyValue id = productEntity.get(productMetadata.field("id"));
        assertNotNull(id);
        assertEquals("abcd", id.asText());
        SpeedyValue name = productEntity.get(productMetadata.field("name"));
        assertNotNull(name);
        assertEquals("na", name.asText());
        assertFalse(productEntity.has(productMetadata.field("productItem")));
    }
}
