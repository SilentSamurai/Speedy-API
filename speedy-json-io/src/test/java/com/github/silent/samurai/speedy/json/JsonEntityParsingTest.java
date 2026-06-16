package com.github.silent.samurai.speedy.json;

import com.github.silent.samurai.speedy.data.*;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.StructureReader;
import com.github.silent.samurai.speedy.json.request.JsonRequestReader;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.serialization.StructureToSpeedy;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/// JSON entity parsing tests. Exercise the streaming read path end-to-end:
/// the format-agnostic {@link StructureToSpeedy} builder driving a JSON
/// {@code StructureReader} from {@link JsonRequestReader} over real entity metadata.
/// Primary-key derivation now happens off the parsed entity ({@code toKey} /
/// {@code isKeyComplete}) rather than a separate pk pass.
class JsonEntityParsingTest {

    private final StructureToSpeedy builder = new StructureToSpeedy();
    private final JsonRequestReader requestReader = new JsonRequestReader();

    private SpeedyEntity parse(EntityMetadata entityMetadata, String json) throws SpeedyHttpException {
        try (StructureReader r = requestReader.readDocument(json.getBytes(StandardCharsets.UTF_8))) {
            r.begin();
            return builder.fromEntity(entityMetadata, r);
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
