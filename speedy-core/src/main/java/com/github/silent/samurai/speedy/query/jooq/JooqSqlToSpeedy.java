package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.ExpansionPathTracker;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.Speedy;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class JooqSqlToSpeedy {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqSqlToSpeedy.class);

    private final DSLContext dslContext;
    private final JooqToJooqSql jooqToJooqSql;
    private final SQLDialect dialect;

    public JooqSqlToSpeedy(DSLContext dslContext) {
        this.dslContext = dslContext;
        this.jooqToJooqSql = new JooqToJooqSql(dslContext);
        this.dialect = dslContext.dialect();
    }

    public SpeedyEntity fromRecord(Record record, EntityMetadata from, Set<String> expand) throws SpeedyHttpException {
        ExpansionPathTracker pathTracker = new ExpansionPathTracker(expand);
        return fromRecordInner(record, from, pathTracker);
    }

    private SpeedyEntity fromRecordInner(Record record, EntityMetadata entityMetadata, ExpansionPathTracker pathTracker) throws SpeedyHttpException {
        SpeedyEntity speedyEntity = SpeedyValueFactory.fromEntityMetadata(entityMetadata);
        
        // Push the current entity onto the path tracker
        pathTracker.pushEntity(entityMetadata);
        
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (fieldMetadata.isAssociation()) {
                String associationName = fieldMetadata.getAssociationMetadata().getName();
                
                // Check if this specific path should be expanded using dot notation
                if (pathTracker.shouldExpand(fieldMetadata.getAssociationMetadata())) {
                    // extract FK from the current record, then query foreign table rows
                    Optional<Result<Record>> associatedRecord = jooqToJooqSql.findByFK(fieldMetadata, record);
                    // if fk is null
                    if (associatedRecord.isEmpty() || associatedRecord.get().isEmpty()) {
                        speedyEntity.put(fieldMetadata, Speedy.fromNull());
                        continue;
                    }
                    if (fieldMetadata.isCollection()) {
                        throw new BadRequestException("operation not supported");
                    } else {
                        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
                        SpeedyEntity associatedEntity = fromRecordInner(
                                associatedRecord.get().get(0),
                                associationMetadata,
                                pathTracker
                        );
                        speedyEntity.put(fieldMetadata, associatedEntity);
                    }
                } else {
                    if (fieldMetadata.isCollection()) {
                        throw new BadRequestException("operation not supported");
                    } else {
                        Optional<SpeedyEntityKey> associatedEntityKey = createSpeedyKeyFromFK(record, fieldMetadata);
                        if (associatedEntityKey.isEmpty() || associatedEntityKey.get().isEmpty()) {
                            speedyEntity.put(fieldMetadata, Speedy.fromNull());
                            continue;
                        }
                        speedyEntity.put(fieldMetadata, associatedEntityKey.get());
                    }
                }
            } else {

                Optional<Object> optionalFieldObj = JooqUtil.getValueFromRecord(record, fieldMetadata, dialect);
                if (optionalFieldObj.isEmpty()) {
                    speedyEntity.put(fieldMetadata, Speedy.fromNull());
                    continue;
                }

                Object fieldValue = optionalFieldObj.get();

                if (fieldMetadata.isCollection()) {
                    Collection<?> listOfInstances = (Collection<?>) fieldValue;
                    List<SpeedyValue> listOfSpeedyValue = new LinkedList<>();
                    for (Object item : listOfInstances) {
                        SpeedyValue speedyValue = SpeedyValueFactory.toSpeedyValue(fieldMetadata, item);
                        listOfSpeedyValue.add(speedyValue);
                    }
                    SpeedyCollection speedyCollection = SpeedyValueFactory.fromCollection(listOfSpeedyValue);
                    speedyEntity.put(fieldMetadata, speedyCollection);
                } else {
                    // fieldValue some time is of not correct type, int promoted to decimal. and visa-versa
                    SpeedyValue speedyValue = SpeedyValueFactory.toSpeedyValue(fieldMetadata, fieldValue);
                    speedyEntity.put(fieldMetadata, speedyValue);
                }
            }
        }
        
        // Pop current entity from the path tracker when done processing
        pathTracker.popEntity();
        return speedyEntity;
    }

    public Optional<SpeedyEntityKey> createSpeedyKeyFromFK(Record record, FieldMetadata fieldMetadata) throws SpeedyHttpException {
        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
        KeyFieldMetadata keyFieldMetadata = associationMetadata.getKeyFields().stream().findAny().orElseThrow();
        // foreign key column
        Optional<Object> optional = JooqUtil.getValueFromRecord(record, fieldMetadata, dialect);
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        SpeedyEntityKey speedyEntityKey = SpeedyValueFactory.createEntityKey(associationMetadata);
        SpeedyValue speedyValue = SpeedyValueFactory.toSpeedyValue(keyFieldMetadata, optional.get());
        speedyEntityKey.put(keyFieldMetadata, speedyValue);

        return Optional.of(speedyEntityKey);
    }

}
