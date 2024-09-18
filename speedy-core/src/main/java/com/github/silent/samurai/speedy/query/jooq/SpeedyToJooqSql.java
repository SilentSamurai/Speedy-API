package com.github.silent.samurai.speedy.query.jooq;


import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SpeedyToJooqSql {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyToJooqSql.class);

    private final DSLContext dslContext;

    public SpeedyToJooqSql(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public Result<Record> findByPrimaryKey(SpeedyEntityKey pk) throws SpeedyHttpException {
        EntityMetadata entityMetadata = pk.getMetadata();
        SelectJoinStep<Record> query = dslContext.select()
                .from(entityMetadata.getDbTableName());

        // Build the where clause for each primary key field
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            SpeedyValue speedyValue = pk.get(keyFieldMetadata);
            Object value = SpeedyValueFactory.toJavaTypeOnlyViaValueType(
                    keyFieldMetadata.getValueType(),
                    speedyValue
            );
            Field<Object> field = JooqUtil.getColumn(keyFieldMetadata);

            query.where(field.equal(value));
        }

        LOGGER.info("findByPrimaryKey query: {}", query);
        return query.fetch();
    }

    public boolean insertEntity(SpeedyEntity entity) throws SpeedyHttpException {
        EntityMetadata entityMetadata = entity.getMetadata();
        InsertSetStep<Record> insertQuery = dslContext
                .insertInto(DSL.table(entityMetadata.getDbTableName()));

        InsertSetMoreStep<Record> returnQuery = null;

        // Build the insert query with field values
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {

//            String dbColumn = fieldMetadata.getDbColumnName().toUpperCase();

            if (fieldMetadata instanceof KeyFieldMetadata && ((KeyFieldMetadata) fieldMetadata).shouldGenerateKey()) {
                UUID value = UUID.randomUUID();
                Field<String> field = JooqUtil.getColumn(fieldMetadata);
                returnQuery = insertQuery.set(field, value.toString());
                entity.put(fieldMetadata, SpeedyValueFactory.fromText(value.toString()));
            } else {

                if (!entity.has(fieldMetadata) || entity.get(fieldMetadata).isEmpty() || entity.get(fieldMetadata).isNull()) {
                    continue;
                }
                SpeedyValue speedyValue = entity.get(fieldMetadata);

                if (fieldMetadata.isAssociation()) {
                    SpeedyEntity associatedEntity = speedyValue.asObject();
                    FieldMetadata associatedFieldMetadata = fieldMetadata.getAssociatedFieldMetadata();
                    SpeedyValue innerValue = associatedEntity.get(associatedFieldMetadata);

                    Object value = SpeedyValueFactory.toJavaTypeOnlyViaValueType(
                            associatedFieldMetadata.getValueType(),
                            innerValue
                    );

                    Field<Object> field = JooqUtil.getColumn(fieldMetadata);
                    returnQuery = insertQuery.set(field, value);
                } else {
                    Object value = SpeedyValueFactory.toJavaTypeOnlyViaValueType(
                            fieldMetadata.getValueType(),
                            speedyValue
                    );
                    Field<Object> field = JooqUtil.getColumn(fieldMetadata);
                    returnQuery = insertQuery.set(field, value);
                }
            }
        }

        LOGGER.info("Insert query: {}", returnQuery);
        if (returnQuery != null) {
            LOGGER.info("bind values: {}", returnQuery.getBindValues());
            returnQuery.execute();
        }

        return true;
    }

    public boolean updateEntity(SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException {
        EntityMetadata entityMetadata = entity.getMetadata();
        UpdateSetFirstStep<Record> updateQuery = dslContext.update(DSL.table(entityMetadata.getDbTableName()));
        UpdateSetMoreStep<Record> returnQuery = null;

        // Set values to be updated
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            SpeedyValue val = entity.get(fieldMetadata);
            if (val != null && !val.isEmpty() && !val.isNull()) {
                Object value = SpeedyValueFactory.toJavaTypeOnlyViaValueType(
                        fieldMetadata.getValueType(),
                        val
                );
                Field<Object> field = JooqUtil.getColumn(fieldMetadata);

                returnQuery = updateQuery.set(field, value);
            }
        }

        // Build where clause based on primary key fields
        if (returnQuery != null) {
            for (KeyFieldMetadata keyFieldMetadata : pk.getMetadata().getKeyFields()) {

                SpeedyValue speedyValue = pk.get(keyFieldMetadata);
                Object value = SpeedyValueFactory.toJavaTypeOnlyViaValueType(
                        keyFieldMetadata.getValueType(),
                        speedyValue
                );

                Field<Object> field = JooqUtil.getColumn(keyFieldMetadata);

                returnQuery.where(field.equal(value));
            }
            returnQuery.execute();
        }
        LOGGER.info("Update query: {}", returnQuery);

        return true;
    }

    public boolean deleteEntity(SpeedyEntityKey pk) throws SpeedyHttpException {
        EntityMetadata entityMetadata = pk.getMetadata();
        DeleteQuery<Record> deleteQuery = dslContext.deleteQuery(DSL.table(entityMetadata.getDbTableName()));

        boolean isConditionProvided = false;

        // Add where conditions based on primary key fields
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            SpeedyValue speedyValue = pk.get(keyFieldMetadata);
            Object value = SpeedyValueFactory.toJavaTypeOnlyViaValueType(
                    keyFieldMetadata.getValueType(),
                    speedyValue
            );

            Field<Object> field = JooqUtil.getColumn(keyFieldMetadata);

            deleteQuery.addConditions(field.equal(value));
            isConditionProvided = true;
        }
        LOGGER.info("Delete query: {}", deleteQuery);
        if (isConditionProvided) {
            deleteQuery.execute();
        }
        return true;
    }

}
