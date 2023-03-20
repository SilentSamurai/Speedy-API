package com.github.silent.samurai.helpers;

import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MetadataUtilTest {

    @Mock
    EntityMetadata entityMetadata;

    @Mock
    FieldMetadata fieldMetadata;

    @BeforeEach
    void setUp() {

    }

    @Test
    void isPrimaryKeyComplete() {
        Mockito.when(entityMetadata.getKeyFields()).thenReturn(Sets.newHashSet("id"));
        HashSet<String> fields = Sets.newHashSet("id");
        assertTrue(MetadataUtil.isPrimaryKeyComplete(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields() {
        Mockito.when(entityMetadata.getKeyFields()).thenReturn(Sets.newHashSet("id"));
        HashSet<String> fields = Sets.newHashSet("id");
        assertTrue(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields1() {
        Mockito.when(entityMetadata.getKeyFields()).thenReturn(Sets.newHashSet("id"));
        HashSet<String> fields = Sets.newHashSet("id", "name");
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields2() {
        Mockito.when(entityMetadata.getKeyFields()).thenReturn(Sets.newHashSet("id"));
        HashSet<String> fields = Sets.newHashSet();
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields3() {
        Mockito.when(entityMetadata.getKeyFields()).thenReturn(Sets.newHashSet("id", "name"));
        HashSet<String> fields = Sets.newHashSet("id");
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void hasOnlyPrimaryKeyFields4() {
        Mockito.when(entityMetadata.getKeyFields()).thenReturn(Sets.newHashSet());
        HashSet<String> fields = Sets.newHashSet("id");
        assertFalse(MetadataUtil.hasOnlyPrimaryKeyFields(entityMetadata, fields));
    }

    @Test
    void getPrimaryKey() throws Exception {
//        Mockito.when(entityMetadata.getKeyFields()).thenReturn(Sets.newHashSet("id"));
        Mockito.when(entityMetadata.getKeyClass()).thenAnswer(inv -> String.class);

        Map<String, String> keys = Maps.newHashMap("id", "1234");
        Object primaryKey = MetadataUtil.createEntityKeyFromMap(entityMetadata, keys);
        assertEquals("1234", primaryKey);
    }

    static class PrimaryKeyTestClass {
        String id;
        String name;
    }

    @Disabled
    @Test
    void getPrimaryKey1() throws Exception {
        Mockito.when(entityMetadata.getKeyFields()).thenReturn(Sets.newHashSet("id", "name"));
        Mockito.when(entityMetadata.getKeyClass()).thenAnswer(inv -> PrimaryKeyTestClass.class);
        Mockito.when(entityMetadata.field(Mockito.anyString())).thenReturn(fieldMetadata);
        Mockito.when(fieldMetadata.getOutputPropertyName()).thenReturn("name");

        HashMap hashMap = new Gson().fromJson("{'id':'abcd', 'name':'na'}", HashMap.class);

        PrimaryKeyTestClass primaryKey = (PrimaryKeyTestClass) MetadataUtil.createEntityKeyFromMap(entityMetadata, hashMap);
        assertEquals("abcd", primaryKey.id);
        assertEquals("na", primaryKey.name);
    }
}