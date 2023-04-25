package com.github.silent.samurai.helpers;

import com.github.silent.samurai.data.*;
import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.parser.SpeedyUriParser;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
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
        SpeedyUriParser speedyUriParser = new SpeedyUriParser(metaModelProcessor, "/Category(id='1234')");
        speedyUriParser.parse();
        Object primaryKey = MetadataUtil.createIdentifierFromParser(speedyUriParser);
        assertEquals("1234", primaryKey);
    }

    @Test
    void createEntityKeyFromMap1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(entityMetadata);
        SpeedyUriParser speedyUriParser = new SpeedyUriParser(metaModelProcessor, "/Category(id='1234', name='na')");
        speedyUriParser.parse();
        PrimaryKeyTestClass primaryKey = (PrimaryKeyTestClass) MetadataUtil.createIdentifierFromParser(speedyUriParser);
        assertEquals("1234", primaryKey.getId());
        assertEquals("na", primaryKey.getName());
    }

    @Test
    void createEntityKeyFromMap2() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
                    Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(entityMetadata);
                    SpeedyUriParser speedyUriParser = new SpeedyUriParser(metaModelProcessor, "/Category(name='1234')");
                    speedyUriParser.parse();
                    MetadataUtil.createIdentifierFromParser(speedyUriParser);
                }
        );
    }

    @Test
    void createEntityKeyFromMap3() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
                    Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(entityMetadata);
                    SpeedyUriParser speedyUriParser = new SpeedyUriParser(metaModelProcessor, "/Category(name='na')");
                    speedyUriParser.parse();
                    MetadataUtil.createIdentifierFromParser(speedyUriParser);
                }
        );
    }


    @Test
    void createEntityKeyFromJSON() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
        JsonElement jsonElement = CommonUtil.getGson().fromJson("{'id':'1234', 'name':'na'}", JsonElement.class);
        Object primaryKey = MetadataUtil.createIdentifierFromJSON(entityMetadata, jsonElement.getAsJsonObject());
        assertEquals("1234", primaryKey);
    }

    @Test
    void createEntityKeyFromJSON1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        JsonElement jsonElement = CommonUtil.getGson().fromJson("{'id':'1234', 'name':'na'}", JsonElement.class);
        PrimaryKeyTestClass primaryKey = (PrimaryKeyTestClass) MetadataUtil.createIdentifierFromJSON(entityMetadata, jsonElement.getAsJsonObject());
        assertEquals("1234", primaryKey.getId());
        assertEquals("na", primaryKey.getName());
    }

    @Test
    void createEntityKeyFromJSON2() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
                    JsonElement jsonElement = CommonUtil.getGson().fromJson("{'name':'na'}", JsonElement.class);
                    MetadataUtil.createIdentifierFromJSON(entityMetadata, jsonElement.getAsJsonObject());
                }
        );
    }

    @Test
    void createEntityKeyFromJSON3() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
                    JsonElement jsonElement = CommonUtil.getGson().fromJson("{'name':'na'}", JsonElement.class);
                    MetadataUtil.createIdentifierFromJSON(entityMetadata, jsonElement.getAsJsonObject());
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
        JsonElement jsonElement = CommonUtil.getGson().fromJson("{'id':'abcd', 'name':'na', 'category':'cat-1'}", JsonElement.class);
        EntityTestClass entity = (EntityTestClass) MetadataUtil.createEntityFromJSON(entityMetadata, jsonElement.getAsJsonObject(), entityManager);
        assertEquals("abcd", entity.getId());
        assertEquals("na", entity.getName());
        assertEquals("cat-1", entity.getCategory());
    }

    @Test
    void createEntityObjectFromJSON1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        JsonElement jsonElement = CommonUtil.getGson().fromJson("{'id':'abcd', 'name':'na', 'category':'cat-1'}", JsonElement.class);
        EntityCompositeKeyTestClass entity = (EntityCompositeKeyTestClass) MetadataUtil.createEntityFromJSON(entityMetadata, jsonElement.getAsJsonObject(), entityManager);
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

        MetadataUtil.updateEntityFromJSON(entityMetadata, entityManager, jsonElement.getAsJsonObject(), entity);
        assertEquals("cat-2", entity.getCategory());
    }*/

    @Test
    void createEntityFromJson2() throws Exception {
        AssociationEntity associationEntity = new AssociationEntity();
        Mockito.when(entityManager.find(AssociationEntity.class, "abcd")).thenReturn(associationEntity);
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(AssociatedEntityTestClass.class);
        JsonElement jsonElement = CommonUtil.getGson().fromJson("{'id':'abcd', 'name':'na', 'category':'cat-1', 'associationEntity':{'id':'abcd'} }", JsonElement.class);
        AssociatedEntityTestClass entity = (AssociatedEntityTestClass) MetadataUtil.createEntityFromJSON(entityMetadata, jsonElement.getAsJsonObject(), entityManager);
        assertEquals("abcd", entity.getId());
        assertEquals("na", entity.getName());
        assertEquals("cat-1", entity.getCategory());
        assertEquals(associationEntity, entity.getAssociationEntity());
    }

}