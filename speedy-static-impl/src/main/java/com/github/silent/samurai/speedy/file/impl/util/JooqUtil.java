package com.github.silent.samurai.speedy.file.impl.util;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.file.impl.metadata.FileFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import org.jooq.DataType;
import org.jooq.Record;
import org.jooq.impl.SQLDataType;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class JooqUtil {


    public static DataType<?> getSQLDataType(ValueType valueType) {
        switch (valueType) {
            case BOOL:
                return SQLDataType.BOOLEAN;
            case TEXT:
                return SQLDataType.NVARCHAR;
            case INT:
                return SQLDataType.INTEGER;
            case FLOAT:
                return SQLDataType.DOUBLE;
            case DATE:
                return SQLDataType.DATE;
            case TIME:
                return SQLDataType.TIME;
            case DATE_TIME:
                return SQLDataType.LOCALDATETIME;
            case ZONED_DATE_TIME:
                return SQLDataType.TIMESTAMPWITHTIMEZONE;
            case OBJECT:
            case COLLECTION:
            case NULL:
            default:
                throw new RuntimeException("DataType not supported: " + valueType);
        }
    }

    public static SpeedyEntity fromJpaEntity(Record record, EntityMetadata from, Set<String> expand) throws Exception {
        return fromJpaEntityInner(record, from, expand);
    }

    private static SpeedyEntity fromJpaEntityInner(Record record, EntityMetadata entityMetadata, Set<String> expands) throws Exception {
        SpeedyEntity speedyEntity = SpeedyValueFactory.fromEntityMetadata(entityMetadata);
        for (FieldMetadata simpleMetadata : entityMetadata.getAllFields()) {
            FileFieldMetadata fieldMetadata = (FileFieldMetadata) simpleMetadata;
            Object fieldValue = record.getValue(fieldMetadata.getDbColumnName());
            if (fieldValue == null) {
                speedyEntity.put(fieldMetadata, SpeedyValueFactory.fromNull());
                continue;
            }

            if (fieldMetadata.isAssociation()) {
//                if (expands.contains(fieldMetadata.getAssociationMetadata().getName())) {
//                    if (fieldMetadata.isCollection()) {
//                        Collection<?> collection = (Collection<?>) fieldValue;
//                        Collection<SpeedyValue> collect = new LinkedList<>();
//                        for (Object item : collection) {
//                            EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
//                            SpeedyEntity associatedEntity = fromJpaEntityInner(item, associationMetadata, expands);
//                            collect.add(associatedEntity);
//                        }
//                        SpeedyCollection speedyCollection = SpeedyValueFactory.fromCollection(collect);
//                        speedyEntity.put(fieldMetadata, speedyCollection);
//                    } else {
//                        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
//                        SpeedyEntity associatedEntity = fromJpaEntityInner(fieldValue, associationMetadata, expands);
//                        speedyEntity.put(fieldMetadata, associatedEntity);
//                    }
//                } else {
//                    if (fieldMetadata.isCollection()) {
//                        Collection<?> collection = (Collection<?>) fieldValue;
//                        Collection<SpeedyValue> collect = new LinkedList<>();
//                        for (Object item : collection) {
//                            EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
//                            SpeedyEntity associatedEntity = fromKey(item, associationMetadata);
//                            collect.add(associatedEntity);
//                        }
//                        SpeedyCollection speedyCollection = SpeedyValueFactory.fromCollection(collect);
//                        speedyEntity.put(fieldMetadata, speedyCollection);
//                    } else {
//                        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
//                        SpeedyEntityKey associatedEntity = fromKey(fieldValue, associationMetadata);
//                        speedyEntity.put(fieldMetadata, associatedEntity);
//                    }
//                }
            } else {
                if (fieldMetadata.isCollection()) {
                    Collection<?> listOfInstances = (Collection<?>) fieldValue;
                    List<SpeedyValue> listOfSpeedyValue = new LinkedList<>();
                    for (Object item : listOfInstances) {
                        SpeedyValue speedyValue = SpeedyValueFactory.fromJavaTypes(fieldMetadata, item);
                        listOfSpeedyValue.add(speedyValue);
                    }
                    SpeedyCollection speedyCollection = SpeedyValueFactory.fromCollection(listOfSpeedyValue);
                    speedyEntity.put(fieldMetadata, speedyCollection);
                } else {
                    SpeedyValue speedyValue = SpeedyValueFactory.fromJavaTypes(fieldMetadata, fieldValue);
                    speedyEntity.put(fieldMetadata, speedyValue);
                }
            }
        }
        return speedyEntity;
    }

}
