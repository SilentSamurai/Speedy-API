package com.github.silent.samurai.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.data.*;
import com.github.silent.samurai.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.HashSet;

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
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
        HashSet<String> fields = Sets.newHashSet("id");
        assertTrue(MetadataUtil.isPrimaryKeyComplete(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields() {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
        HashSet<String> fields = Sets.newHashSet("id");
        assertTrue(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields1() {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
        HashSet<String> fields = Sets.newHashSet("id", "name");
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields2() {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
        HashSet<String> fields = Sets.newHashSet();
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields2_1() {
        EntityMetadata entityMetadata = Mockito.mock(EntityMetadata.class);
//        Mockito.when(entityMetadata.getKeyFields()).thenReturn(Sets.newHashSet());
        HashSet<String> fields = Sets.newHashSet();
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }


    @Test
    void hasOnlyPrimaryKeyFields3() {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        HashSet<String> fields = Sets.newHashSet("id");
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields4() {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        HashSet<String> fields = Sets.newHashSet("id", "name", "category");
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void createEntityKeyFromMap() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
        Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(entityMetadata);
        SpeedyUriContext speedyUriContext = new SpeedyUriContext(metaModelProcessor, "/Category(id='1234')");
        speedyUriContext.parse();
        Object primaryKey = MetadataUtil.createIdentifierFromParser(speedyUriContext);
        assertEquals("1234", primaryKey);
    }

    @Test
    void createEntityKeyFromMap1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(entityMetadata);
        SpeedyUriContext speedyUriContext = new SpeedyUriContext(metaModelProcessor, "/Category(id='1234', name='na')");
        speedyUriContext.parse();
        PrimaryKeyTestClass primaryKey = (PrimaryKeyTestClass) MetadataUtil.createIdentifierFromParser(speedyUriContext);
        assertEquals("1234", primaryKey.getId());
        assertEquals("na", primaryKey.getName());
    }

    @Test
    void createEntityKeyFromMap2() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
                    Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(entityMetadata);
                    SpeedyUriContext speedyUriContext = new SpeedyUriContext(metaModelProcessor, "/Category(name='1234')");
                    speedyUriContext.parse();
                    MetadataUtil.createIdentifierFromParser(speedyUriContext);
                }
        );
    }

    @Test
    void createEntityKeyFromMap3() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
                    Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(entityMetadata);
                    SpeedyUriContext speedyUriContext = new SpeedyUriContext(metaModelProcessor, "/Category(name='na')");
                    speedyUriContext.parse();
                    MetadataUtil.createIdentifierFromParser(speedyUriContext);
                }
        );
    }


    @Test
    void createEntityKeyFromJSON() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'1234', 'name':'na'}");
        Object primaryKey = MetadataUtil.createIdentifierFromJSON(entityMetadata, (ObjectNode) jsonElement);
        assertEquals("1234", primaryKey);
    }

    @Test
    void createEntityKeyFromJSON1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'1234', 'name':'na'}");
        PrimaryKeyTestClass primaryKey = (PrimaryKeyTestClass) MetadataUtil.createIdentifierFromJSON(entityMetadata, (ObjectNode) jsonElement);
        assertEquals("1234", primaryKey.getId());
        assertEquals("na", primaryKey.getName());
    }

    @Test
    void createEntityKeyFromJSON2() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
                    JsonNode jsonElement = CommonUtil.json().readTree("{'name':'na'}");
                    MetadataUtil.createIdentifierFromJSON(entityMetadata, (ObjectNode) jsonElement);
                }
        );
    }

    @Test
    void createEntityKeyFromJSON3() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
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
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'abcd', 'name':'na', 'category':'cat-1'}");
        EntityTestClass entity = (EntityTestClass) MetadataUtil.createEntityFromJSON(entityMetadata, (ObjectNode) jsonElement, entityManager);
        assertEquals("abcd", entity.getId());
        assertEquals("na", entity.getName());
        assertEquals("cat-1", entity.getCategory());
    }

    @Test
    void createEntityObjectFromJSON1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'abcd', 'name':'na', 'category':'cat-1'}");
        EntityCompositeKeyTestClass entity = (EntityCompositeKeyTestClass) MetadataUtil.createEntityFromJSON(entityMetadata, (ObjectNode) jsonElement, entityManager);
        assertEquals("abcd", entity.getId());
        assertEquals("na", entity.getName());
        assertEquals("cat-1", entity.getCategory());
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
        AssociationEntity associationEntity = new AssociationEntity();
        Mockito.when(entityManager.find(AssociationEntity.class, "abcd")).thenReturn(associationEntity);
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(AssociatedEntityTestClass.class);
        JsonNode jsonElement = CommonUtil.json().readTree("{'id':'abcd', 'name':'na', 'category':'cat-1', 'associationEntity':{'id':'abcd'} }");
        AssociatedEntityTestClass entity = (AssociatedEntityTestClass) MetadataUtil.createEntityFromJSON(entityMetadata, (ObjectNode) jsonElement, entityManager);
        assertEquals("abcd", entity.getId());
        assertEquals("na", entity.getName());
        assertEquals("cat-1", entity.getCategory());
        assertEquals(associationEntity, entity.getAssociationEntity());
    }

}