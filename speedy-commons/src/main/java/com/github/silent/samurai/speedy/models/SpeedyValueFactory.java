package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


public class SpeedyValueFactory {

    public static SpeedyText fromText(String value) {
        return new SpeedyText(value);
    }

    public static SpeedyInt fromInt(Integer value) {
        return new SpeedyInt(value);
    }

    public static SpeedyDouble fromDouble(Double value) {
        return new SpeedyDouble(value);
    }

    public static SpeedyDate fromDate(LocalDate value) {
        return new SpeedyDate(value);
    }

    public static SpeedyTime fromTime(LocalTime value) {
        return new SpeedyTime(value);
    }

    public static SpeedyDateTime fromDateTime(LocalDateTime value) {
        return new SpeedyDateTime(value);
    }

    public static SpeedyCollection fromCollection(Collection<SpeedyValue> value) {
        return new SpeedyCollection(value);
    }



    public static SpeedyValue fromValueTypePrimitive(ValueType type, Object instance) {
        switch (type) {
            case TEXT:
                return fromText((String) instance);
            case INT:
                return fromInt((Integer) instance);
            case FLOAT:
                return fromDouble((Double) instance);
            case DATE:
                return fromDate((LocalDate) instance);
            case TIME:
                return fromTime((LocalTime) instance);
            case DATE_TIME:
                return fromDateTime((LocalDateTime) instance);
            default:
                return SpeedyNull.SPEEDY_NULL;
        }
    }

    public static SpeedyValue fromEntityMetadata(EntityMetadata entityMetadata, Object instance) {
        SpeedyEntity speedyEntity = new SpeedyEntity(entityMetadata);
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
//            fromFieldMetadata(fieldMetadata, )
//            speedyEntity.put(fieldMetadata, );
        }
        return speedyEntity;
    }

    public static SpeedyValue fromFieldMetadata(FieldMetadata fieldMetadata, Object instance) {
        if (fieldMetadata.isAssociation()) {
            if (fieldMetadata.isCollection()) {
                Collection<?> collection = (Collection<?>) instance;
                List<SpeedyValue> speedyValues = collection.stream().map(obj -> fromEntityMetadata(fieldMetadata.getAssociationMetadata(), obj)).collect(Collectors.toList());
                return fromCollection(speedyValues);
            } else {
                return fromEntityMetadata(fieldMetadata.getAssociationMetadata(), instance);
            }
        } else {
            if (fieldMetadata.isCollection()) {
                Collection<?> collection = (Collection<?>) instance;
                List<SpeedyValue> speedyValues = collection.stream().map(obj -> fromValueTypePrimitive(fieldMetadata.getValueType(), obj)).collect(Collectors.toList());
                return fromCollection(speedyValues);
            } else {
                return fromValueTypePrimitive(fieldMetadata.getValueType(), instance);
            }
        }
    }
}
