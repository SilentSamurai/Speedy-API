package com.github.silent.samurai.speedy.serialization;

import com.github.silent.samurai.speedy.data.*;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.request.StructureReader;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/// Entity parsing tests for the format-agnostic {@link StructureToSpeedy} builder, driven by an
/// {@link InMemoryStructureReader} (no format module). Exercise key derivation
/// ({@code toKey} / {@code isKeyComplete}), associations, and null handling over real entity
/// metadata. The JSON strings are only a convenient way to build the input token tree.
class EntityParsingTest {

    private final StructureToSpeedy builder = new StructureToSpeedy();

    private SpeedyEntity parse(EntityMetadata entityMetadata, String json) throws SpeedyHttpException {
        StructureReader r = new InMemoryStructureReader(readTree(json));
        r.begin();
        return builder.fromEntity(entityMetadata, r);
    }

    private Object readTree(String json) throws SpeedyHttpException {
        try {
            return CommonUtil.json().readValue(json, Object.class);
        } catch (IOException e) {
            throw new BadRequestException("invalid test json", e);
        }
    }

    @Test
    void createEntityKeyFromJSON() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        SpeedyEntity entity = parse(entityMetadata, "{'id':'1234', 'name':'na'}");
        SpeedyEntityKey primaryKey = builder.toKey(entityMetadata, entity);
        assertTrue(builder.isKeyComplete(entityMetadata, entity));
        assertEquals("1234", primaryKey.get(entityMetadata.field("id")).asText());
    }

    @Test
    void createEntityKeyFromJSON1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
        SpeedyEntity entity = parse(entityMetadata, "{'id':'1234', 'name':'na'}");
        SpeedyEntityKey primaryKey = builder.toKey(entityMetadata, entity);
        assertTrue(builder.isKeyComplete(entityMetadata, entity));
        assertEquals("1234", primaryKey.get(entityMetadata.field("id")).asText());
        assertEquals("na", primaryKey.get(entityMetadata.field("name")).asText());
    }

    @Test
    void incompleteKeyDetected() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        SpeedyEntity entity = parse(entityMetadata, "{'name':'na'}");
        assertFalse(builder.isKeyComplete(entityMetadata, entity));
    }

    @Test
    void incompleteCompositeKeyDetected() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
        SpeedyEntity entity = parse(entityMetadata, "{'name':'na'}");
        assertFalse(builder.isKeyComplete(entityMetadata, entity));
    }

    @Test
    void createEntityObjectFromJSON() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        SpeedyEntity entity = parse(entityMetadata, "{'id':'abcd', 'name':'na', 'category':'cat-1'}");
        assertEquals("abcd", entity.get(entityMetadata.field("id")).asText());
        assertEquals("na", entity.get(entityMetadata.field("name")).asText());
        assertEquals("cat-1", entity.get(entityMetadata.field("category")).asText());
    }

    @Test
    void createEntityObjectFromJSON1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);
        SpeedyEntity entity = parse(entityMetadata, "{'id':'abcd', 'name':'na', 'category':'cat-1'}");
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
        SpeedyEntity productEntity = parse(productMetadata,
                "{'id':'abcd', 'name':'na', 'category':'cat-1', 'productItem':{'id':'abcd'} }");
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
        SpeedyEntity productEntity = parse(productMetadata,
                "{'id':'abcd', 'name':'na', 'category':'cat-1', 'productItem': null }");
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
