package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import org.jooq.*;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class JooqSqlToSpeedy {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqSqlToSpeedy.class);

    private final DSLContext dslContext;
    private final JooqToJooqSql jooqToJooqSql;

    public JooqSqlToSpeedy(DSLContext dslContext) {
        this.dslContext = dslContext;
        this.jooqToJooqSql = new JooqToJooqSql(dslContext);
    }

    public SpeedyEntity fromRecord(Record record, EntityMetadata from, Set<String> expand) throws SpeedyHttpException {
        return fromRecordInner(record, from, expand);
    }

    private SpeedyEntity fromRecordInner(Record record, EntityMetadata entityMetadata, Set<String> expands) throws SpeedyHttpException {
        SpeedyEntity speedyEntity = SpeedyValueFactory.fromEntityMetadata(entityMetadata);
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {

            if (fieldMetadata.isAssociation()) {
                if (expands.contains(fieldMetadata.getAssociationMetadata().getName())) {
                    Result<Record> associatedRecord = jooqToJooqSql.findByFK(fieldMetadata, record);
                    if (associatedRecord.isEmpty()) {
                        speedyEntity.put(fieldMetadata, SpeedyValueFactory.fromNull());
                        continue;
                    }
                    if (fieldMetadata.isCollection()) {
                        throw new BadRequestException("operation not supported");
                    } else {
                        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                        SpeedyEntity associatedEntity = fromRecordInner(
                                associatedRecord.get(0),
                                associationMetadata,
                                Set.of());
                        speedyEntity.put(fieldMetadata, associatedEntity);
                    }
                } else {
                    if (fieldMetadata.isCollection()) {
                        throw new BadRequestException("operation not supported");
                    } else {
                        SpeedyEntityKey associatedEntityKey = createSpeedyKeyFromFK(record, fieldMetadata);
                        speedyEntity.put(fieldMetadata, associatedEntityKey);
                    }
                }
            } else {

                Optional<Object> optionalFieldObj = JooqUtil.getValueFromRecord(record, fieldMetadata);
                if (optionalFieldObj.isEmpty()) {
                    speedyEntity.put(fieldMetadata, SpeedyValueFactory.fromNull());
                    continue;
                }

                Object fieldValue = optionalFieldObj.get();

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
                    SpeedyValue speedyValue = SpeedyValueFactory.fromJavaTypes(
                            fieldValue.getClass(),
                            fieldMetadata.getValueType(),
                            fieldValue);
                    speedyEntity.put(fieldMetadata, speedyValue);
                }
            }
        }
        return speedyEntity;
    }

    public SpeedyEntityKey createSpeedyKeyFromFK(Record record, FieldMetadata fieldMetadata) throws SpeedyHttpException {
        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();

        SpeedyEntityKey speedyEntityKey = SpeedyValueFactory.createEntityKey(associationMetadata);

        KeyFieldMetadata keyFieldMetadata = associationMetadata.getKeyFields().stream().findAny().orElseThrow();
        // foreign key column
        Optional<Object> optional = JooqUtil.getValueFromRecord(record, fieldMetadata);
        if (optional.isEmpty()) {
            speedyEntityKey.put(keyFieldMetadata, SpeedyValueFactory.fromNull());
        } else {
            SpeedyValue speedyValue = SpeedyValueFactory.fromJavaTypes(keyFieldMetadata, optional.get());
            speedyEntityKey.put(keyFieldMetadata, speedyValue);
        }

        return speedyEntityKey;
    }

}
