package com.github.silent.samurai.speedy.helpers;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.metadata.MetaModelBuilder;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyInt;
import com.github.silent.samurai.speedy.models.SpeedyNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetadataUtilRegressionTest {

    private EntityMetadata buildEntityWithKey() throws NotFoundException {
        MetaModelBuilder builder = MetadataBuilder.builder();
        builder.entity("TestEntity").dbTableName("test_entity");
        builder.ref("TestEntity").keyField("id", "id", ColumnType.INTEGER);
        builder.ref("TestEntity").field("name", "name", ColumnType.VARCHAR);
        return builder.build().findEntityMetadata("TestEntity");
    }

    @Test
    void isKeyCompleteInEntity_returnsFalseForAbsentKey() throws Exception {
        EntityMetadata entityMetadata = buildEntityWithKey();
        SpeedyEntity entity = new SpeedyEntity(entityMetadata);
        assertFalse(MetadataUtil.isKeyCompleteInEntity(entityMetadata, entity));
    }

    @Test
    void isKeyCompleteInEntity_returnsFalseForNullKey() throws Exception {
        EntityMetadata entityMetadata = buildEntityWithKey();
        SpeedyEntity entity = new SpeedyEntity(entityMetadata);
        entity.put(entityMetadata.field("id"), SpeedyNull.SPEEDY_NULL);
        assertFalse(MetadataUtil.isKeyCompleteInEntity(entityMetadata, entity));
    }

    @Test
    void isKeyCompleteInEntity_returnsFalseForEmptyKey() throws Exception {
        EntityMetadata entityMetadata = buildEntityWithKey();
        SpeedyEntity entity = new SpeedyEntity(entityMetadata);
        entity.put(entityMetadata.field("id"), new SpeedyInt(null));
        assertFalse(MetadataUtil.isKeyCompleteInEntity(entityMetadata, entity));
    }

    @Test
    void isKeyCompleteInEntity_returnsTrueForPresentKey() throws Exception {
        EntityMetadata entityMetadata = buildEntityWithKey();
        SpeedyEntity entity = new SpeedyEntity(entityMetadata);
        entity.put(entityMetadata.field("id"), new SpeedyInt(42L));
        assertTrue(MetadataUtil.isKeyCompleteInEntity(entityMetadata, entity));
    }

    @Test
    void isKeyCompleteInEntity_doesNotThrowOnAbsentField() throws Exception {
        EntityMetadata entityMetadata = buildEntityWithKey();
        SpeedyEntity entity = new SpeedyEntity(entityMetadata);
        assertDoesNotThrow(() -> MetadataUtil.isKeyCompleteInEntity(entityMetadata, entity));
    }
}
