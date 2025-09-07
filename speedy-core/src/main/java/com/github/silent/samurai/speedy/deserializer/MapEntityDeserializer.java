package com.github.silent.samurai.speedy.deserializer;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.mappings.TypeConverterRegistry;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyNull;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static com.github.silent.samurai.speedy.utils.SpeedyValueFactory.*;

public record MapEntityDeserializer(Map<String, String> entityMap, EntityMetadata entityMetadata,
                                    EntityManager entityManager) {

    private static SpeedyValue fromBasicString(ValueType valueType, String valueAsString) throws Exception {
        return switch (valueType) {
            case TEXT -> fromText(valueAsString);
            case INT -> {
                Long intValue = TypeConverterRegistry.fromString(valueAsString, Long.class);
                yield fromInt(intValue);
            }
            case FLOAT -> {
                Double aDouble = TypeConverterRegistry.fromString(valueAsString, Double.class);
                yield fromDouble(aDouble);
            }
            case DATE -> fromDate(LocalDate.parse(valueAsString));
            case TIME -> fromTime(LocalTime.parse(valueAsString));
            case DATE_TIME -> fromDateTime(LocalDateTime.parse(valueAsString));
            case BOOL, ZONED_DATE_TIME, OBJECT, COLLECTION, ENUM, ENUM_ORD, NULL -> SpeedyNull.SPEEDY_NULL;
        };
    }

    public SpeedyEntity deserialize() throws Exception {
        return createEntity(this.entityMetadata, entityMap);
    }

    private SpeedyEntity createEntity(EntityMetadata entityMetadata, Map<String, String> entityMap) throws Exception {
        SpeedyEntity entity = new SpeedyEntity(entityMetadata);
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!fieldMetadata.isDeserializable()) continue;
            SpeedyValue value = this.createField(fieldMetadata, entityMap);
            if (value.isPresent()) {
                entity.put(fieldMetadata, value);
            }
        }
        return entity;
    }

    private SpeedyValue createField(
            FieldMetadata fieldMetadata,
            Map<String, String> fieldsMap) throws Exception {
        SpeedyValue value = fromNull();
        String propertyName = fieldMetadata.getOutputPropertyName();
        if (fieldsMap.containsKey(propertyName)) {
            if (fieldMetadata.isAssociation()) {
                // TODO: array of association & association
            } else {
                value = fromBasicString(fieldMetadata.getValueType(), fieldsMap.get(propertyName));
            }
        }
        return value;
    }

//    private Object createEntityKey(EntityMetadata association, Map<String, String> keyMap) throws Exception {
//        MapEntityKeyDeserializer deserializer = new MapEntityKeyDeserializer(keyMap, association);
//        return deserializer.deserialize();
//    }


}
