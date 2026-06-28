package com.github.silent.samurai.speedy.helpers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.data.*;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.metadata.EntityBuilder;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import com.github.silent.samurai.speedy.utils.Speedy;
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

    private static EntityMetadata dbGeneratedKeyEntity() throws NotFoundException {
        EntityBuilder entity = MetadataBuilder.builder().entity("DbGen");
        entity.keyField("id", "ID", ColumnType.INTEGER).insertable(false); // IDENTITY-style
        entity.field("name", "NAME", ColumnType.VARCHAR);
        return entity.build();
    }

    /// An absent DB-generated key is the backend-read-back failure we must catch. (SpeedyEntity.get
    /// throws on an absent field, so this also guards against that footgun.)
    @Test
    void findUnpopulatedDatabaseGeneratedKey_absent() throws NotFoundException {
        EntityMetadata md = dbGeneratedKeyEntity();
        SpeedyEntity entity = new SpeedyEntity(md); // backend "forgot" to read the key back
        assertTrue(MetadataUtil.findUnpopulatedDatabaseGeneratedKey(md, entity).isPresent());
    }

    /// A populated DB-generated key is the happy path — nothing flagged.
    @Test
    void findUnpopulatedDatabaseGeneratedKey_populated() throws NotFoundException {
        EntityMetadata md = dbGeneratedKeyEntity();
        KeyFieldMetadata key = md.getKeyFields().iterator().next();
        SpeedyEntity entity = new SpeedyEntity(md);
        entity.put(key, Speedy.from(5L));
        assertTrue(MetadataUtil.findUnpopulatedDatabaseGeneratedKey(md, entity).isEmpty());
    }

    /// Only DB-generated keys are checked: an app-generated (UUID) key absent from the entity must
    /// not be flagged, since the app — not the database — produces it.
    @Test
    void findUnpopulatedDatabaseGeneratedKey_ignoresAppGeneratedKey() throws NotFoundException {
        EntityBuilder entity = MetadataBuilder.builder().entity("AppGen");
        entity.keyField("id", "ID", ColumnType.UUID).shouldGenerateKey(true);
        entity.field("name", "NAME", ColumnType.VARCHAR);
        EntityMetadata md = entity.build();
        assertTrue(MetadataUtil.findUnpopulatedDatabaseGeneratedKey(md, new SpeedyEntity(md)).isEmpty());
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