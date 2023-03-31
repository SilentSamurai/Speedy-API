package com.github.silent.samurai.helpers;

import com.github.silent.samurai.data.EntityCompositeKeyTestClass;
import com.github.silent.samurai.data.EntityTestClass;
import com.github.silent.samurai.data.PrimaryKeyTestClass;
import com.github.silent.samurai.data.StaticEntityMetadata;
import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MetadataUtilTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataUtilTest.class);

    @BeforeEach
    void setUp() {

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
        Map<String, String> keys = Maps.newHashMap("id", "1234");
        Object primaryKey = MetadataUtil.createEntityKeyFromMap(entityMetadata, keys);
        assertEquals("1234", primaryKey);
    }

    @Test
    void createEntityKeyFromMap1() throws Exception {
        HashMap hashMap = new Gson().fromJson("{'id':'abcd', 'name':'na'}", HashMap.class);
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        PrimaryKeyTestClass primaryKey = (PrimaryKeyTestClass) MetadataUtil.createEntityKeyFromMap(entityMetadata, hashMap);
        assertEquals("abcd", primaryKey.getId());
        assertEquals("na", primaryKey.getName());
    }

    @Test
    void createEntityKeyFromMap2() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
                    Map<String, String> keys = Maps.newHashMap("name", "1234");
                    MetadataUtil.createEntityKeyFromMap(entityMetadata, keys);
                }
        );
    }

    @Test
    void createEntityKeyFromMap3() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
                    HashMap hashMap = new Gson().fromJson("{'name':'na'}", HashMap.class);
                    MetadataUtil.createEntityKeyFromMap(entityMetadata, hashMap);
                }
        );
    }


    @Test
    void createEntityKeyFromJSON() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
        JsonElement jsonElement = CommonUtil.getGson().fromJson("{'id':'1234', 'name':'na'}", JsonElement.class);
        Object primaryKey = MetadataUtil.createEntityKeyFromJSON(entityMetadata, jsonElement.getAsJsonObject());
        assertEquals("1234", primaryKey);
    }

    @Test
    void createEntityKeyFromJSON1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        JsonElement jsonElement = CommonUtil.getGson().fromJson("{'id':'1234', 'name':'na'}", JsonElement.class);
        PrimaryKeyTestClass primaryKey = (PrimaryKeyTestClass) MetadataUtil.createEntityKeyFromJSON(entityMetadata, jsonElement.getAsJsonObject());
        assertEquals("1234", primaryKey.getId());
        assertEquals("na", primaryKey.getName());
    }

    @Test
    void createEntityKeyFromJSON2() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
                    JsonElement jsonElement = CommonUtil.getGson().fromJson("{'name':'na'}", JsonElement.class);
                    MetadataUtil.createEntityKeyFromJSON(entityMetadata, jsonElement.getAsJsonObject());
                }
        );
    }

    @Test
    void createEntityKeyFromJSON3() throws Exception {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> {
                    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
                    JsonElement jsonElement = CommonUtil.getGson().fromJson("{'name':'na'}", JsonElement.class);
                    MetadataUtil.createEntityKeyFromJSON(entityMetadata, jsonElement.getAsJsonObject());
                }
        );
    }

    @Test
    void createEntityObjectFromMap() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
        HashMap hashMap = new Gson().fromJson("{'id':'abcd', 'name':'na', 'category':'cat-1'}", HashMap.class);
        EntityTestClass entity = (EntityTestClass) MetadataUtil.createEntityObjectFromMap(entityMetadata, hashMap);
        assertEquals("abcd", entity.getId());
        assertEquals("na", entity.getName());
        assertEquals("cat-1", entity.getCategory());
    }

    @Test
    void createEntityObjectFromMap1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        HashMap hashMap = new Gson().fromJson("{'id':'abcd', 'name':'na', 'category':'cat-1'}", HashMap.class);
        EntityCompositeKeyTestClass entity = (EntityCompositeKeyTestClass) MetadataUtil.createEntityObjectFromMap(entityMetadata, hashMap);
        assertEquals("abcd", entity.getId());
        assertEquals("na", entity.getName());
        assertEquals("cat-1", entity.getCategory());
    }

    @Test
    void createEntityObjectFromJSON() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);
        JsonElement jsonElement = new Gson().fromJson("{'id':'abcd', 'name':'na', 'category':'cat-1'}", JsonElement.class);
        EntityTestClass entity = (EntityTestClass) MetadataUtil.createEntityObjectFromJSON(entityMetadata, jsonElement.getAsJsonObject());
        assertEquals("abcd", entity.getId());
        assertEquals("na", entity.getName());
        assertEquals("cat-1", entity.getCategory());
    }

    @Test
    void createEntityObjectFromJSON1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        JsonElement jsonElement = new Gson().fromJson("{'id':'abcd', 'name':'na', 'category':'cat-1'}", JsonElement.class);
        EntityCompositeKeyTestClass entity = (EntityCompositeKeyTestClass) MetadataUtil.createEntityObjectFromJSON(entityMetadata, jsonElement.getAsJsonObject());
        assertEquals("abcd", entity.getId());
        assertEquals("na", entity.getName());
        assertEquals("cat-1", entity.getCategory());
    }

    @Test
    void updateEntityFromJson1() throws Exception {
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityCompositeKeyTestClass.class);
        HashMap hashMap = new Gson().fromJson("{'id':'abcd', 'name':'na', 'category':'cat-1'}", HashMap.class);
        EntityCompositeKeyTestClass entity = (EntityCompositeKeyTestClass) MetadataUtil.createEntityObjectFromMap(entityMetadata, hashMap);
        assertEquals("abcd", entity.getId());
        assertEquals("na", entity.getName());
        assertEquals("cat-1", entity.getCategory());

        JsonElement jsonElement = new Gson().fromJson("{'category':'cat-2'}", JsonElement.class);

        MetadataUtil.updateEntityFromJson(entityMetadata, jsonElement.getAsJsonObject(), entity);
        assertEquals("cat-2", entity.getCategory());
    }

}