package com.github.silent.samurai.speedy.deserializer;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.io.BasicDeserializer;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyNull;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static com.github.silent.samurai.speedy.utils.SpeedyValueFactory.*;

public class MapEntityDeserializer {

    private final Map<String, String> entityMap;
    private final EntityMetadata entityMetadata;
    private final EntityManager entityManager;

    public MapEntityDeserializer(Map<String, String> entityMap, EntityMetadata entityMetadata, EntityManager entityManager) {
        this.entityMap = entityMap;
        this.entityMetadata = entityMetadata;
        this.entityManager = entityManager;
    }

    public static SpeedyValue fromBasicString(ValueType valueType, String valueAsString) throws Exception {
        switch (valueType) {
            case TEXT:
                return fromText(valueAsString);
            case INT:
                Integer intValue = BasicDeserializer.stringToPrimitive(valueAsString, Integer.class);
                return fromInt(intValue);
            case FLOAT:
                Double aDouble = BasicDeserializer.stringToPrimitive(valueAsString, Double.class);
                return fromDouble(aDouble);
            case DATE:
                return fromDate(LocalDate.parse(valueAsString));
            case TIME:
                return fromTime(LocalTime.parse(valueAsString));
            case DATE_TIME:
                return fromDateTime(LocalDateTime.parse(valueAsString));
            default:
                return SpeedyNull.SPEEDY_NULL;
        }
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
        SpeedyValue value = null;
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
