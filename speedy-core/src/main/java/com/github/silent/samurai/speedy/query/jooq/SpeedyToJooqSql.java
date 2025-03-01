package com.github.silent.samurai.speedy.query.jooq;


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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
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
                .from(JooqUtil.getTable(entityMetadata, dslContext.dialect()));

        // Build the where clause for each primary key field
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            SpeedyValue speedyValue = pk.get(keyFieldMetadata);
            Object value = JooqUtil.toJooqType(
                    speedyValue,
                    keyFieldMetadata.getColumnType()
            );
            Field<Object> field = JooqUtil.getColumn(keyFieldMetadata, dslContext.dialect());

            query.where(field.eq(value));
        }

        LOGGER.info("Executing findByPrimaryKey query for entity '{}': {}", entityMetadata.getName(), query);
        return query.fetch();
    }

    private InsertSetMoreStep<Record> handleAssociation(InsertSetStep<Record> insertQuery,
                                                        FieldMetadata fieldMetadata,
                                                        SpeedyValue speedyValue) throws SpeedyHttpException {
        SpeedyEntity associatedEntity = speedyValue.asObject();
        FieldMetadata associatedFieldMetadata = fieldMetadata.getAssociatedFieldMetadata();
        if (!associatedEntity.has(associatedFieldMetadata)
                || associatedEntity.get(associatedFieldMetadata).isEmpty()
                || associatedEntity.get(associatedFieldMetadata).isNull()) {
            return null;
        }
        SpeedyValue innerValue = associatedEntity.get(associatedFieldMetadata);
        Object value = JooqUtil.toJooqType(innerValue, associatedFieldMetadata.getColumnType());
        return insertQuery.set(JooqUtil.getColumn(fieldMetadata, dslContext.dialect()), value);
    }

    private Optional<InsertSetMoreStep<Record>> handleFieldMetadata(FieldMetadata fieldMetadata,
                                                                    SpeedyEntity entity,
                                                                    InsertSetStep<Record> insertQuery) throws SpeedyHttpException {
        InsertSetMoreStep<Record> returnQuery = null;
        if (fieldMetadata instanceof KeyFieldMetadata && ((KeyFieldMetadata) fieldMetadata).shouldGenerateKey()) {
            UUID value = UUID.randomUUID();
            Field<String> field = JooqUtil.getColumn(fieldMetadata, dslContext.dialect());
            returnQuery = insertQuery.set(field, value.toString());
            entity.put(fieldMetadata, SpeedyValueFactory.fromText(value.toString()));
        } else {
            if (!entity.has(fieldMetadata) || entity.get(fieldMetadata).isEmpty() || entity.get(fieldMetadata).isNull()) {
                // you can throw by checking nullable or db can throw
                return Optional.empty();
            }
            SpeedyValue speedyValue = entity.get(fieldMetadata);
            if (fieldMetadata.isAssociation()) {
                returnQuery = handleAssociation(insertQuery, fieldMetadata, speedyValue);
            } else {
                Object value = JooqUtil.toJooqType(
                        speedyValue,
                        fieldMetadata.getColumnType()
                );
                Field<Object> field = JooqUtil.getColumn(fieldMetadata, dslContext.dialect());
                returnQuery = insertQuery.set(field, value);
            }
        }
        return Optional.ofNullable(returnQuery);
    }

    public void insertEntity(DSLContext context, SpeedyEntity entity) throws SpeedyHttpException {
        EntityMetadata entityMetadata = entity.getMetadata();
        InsertSetStep<Record> insertQuery = context
                .insertInto(DSL.table(entityMetadata.getDbTableName()));
        Optional<InsertSetMoreStep<Record>> returnQuery = Optional.empty();
        // Build the insert query with field values
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            var rq = handleFieldMetadata(fieldMetadata, entity, insertQuery);
            if (rq.isPresent()) {
                returnQuery = rq;
            }
        }
        LOGGER.info("Insert query: {}", returnQuery.orElse(null));
        if (returnQuery.isPresent()) {
            LOGGER.info("bind values: {}", returnQuery.get().getBindValues());
            returnQuery.get().execute();
        }

    }

    public void insertEntity(List<SpeedyEntity> entities) {
        dslContext.transaction(conf -> {
            for (SpeedyEntity entity : entities) {
                insertEntity(conf.dsl(), entity);
            }
        });
    }

    private Optional<UpdateSetMoreStep<Record>> handleAssociation(
            UpdateSetFirstStep<Record> updateQuery,
            FieldMetadata fieldMetadata,
            SpeedyEntity entity) throws SpeedyHttpException {
        //
        SpeedyValue val = entity.get(fieldMetadata);
        SpeedyEntity associatedEntity = val.asObject();
        FieldMetadata associatedFieldMetadata = fieldMetadata.getAssociatedFieldMetadata();
        if (!associatedEntity.has(associatedFieldMetadata)
                || associatedEntity.get(associatedFieldMetadata).isEmpty()
                || associatedEntity.get(associatedFieldMetadata).isNull()) {
            // you can throw by checking nullable or db can throw
            return Optional.empty();
        }
        SpeedyValue innerValue = associatedEntity.get(associatedFieldMetadata);
        Object value = JooqUtil.toJooqType(
                innerValue,
                associatedFieldMetadata.getColumnType()

        );
        // update current object with foreign key
        Field<Object> field = JooqUtil.getColumn(fieldMetadata, dslContext.dialect());
        return Optional.of(updateQuery.set(field, value));
    }


    private Optional<UpdateSetMoreStep<Record>> handleFieldMetadata(
            FieldMetadata fieldMetadata,
            SpeedyEntity entity,
            UpdateSetFirstStep<Record> updateQuery) throws SpeedyHttpException {
        if (entity.has(fieldMetadata)
                && !entity.get(fieldMetadata).isEmpty()
                && !entity.get(fieldMetadata).isNull()) {
            if (fieldMetadata.isAssociation()) {
                return handleAssociation(updateQuery, fieldMetadata, entity);
            } else {
                SpeedyValue val = entity.get(fieldMetadata);
                Object value = JooqUtil.toJooqType(
                        val,
                        fieldMetadata.getColumnType()
                );
                Field<Object> field = JooqUtil.getColumn(fieldMetadata, dslContext.dialect());
                return Optional.of(updateQuery.set(field, value));
            }
        }
        return Optional.empty();
    }

    private void updateEntityInTransaction(DSLContext dsl, SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException {
        EntityMetadata entityMetadata = entity.getMetadata();
        UpdateSetFirstStep<Record> updateQuery = dsl.update(DSL.table(entityMetadata.getDbTableName()));
        Optional<UpdateSetMoreStep<Record>> returnQuery = Optional.empty();

        // Set values to be updated
        for (FieldMetadata fieldMetadata : entityMetadata.getAllNonKeyFields()) {
            var rq = handleFieldMetadata(fieldMetadata, entity, updateQuery);
            if (rq.isPresent()) {
                returnQuery = rq;
            }
        }
        // Build where clause based on primary key fields
        if (returnQuery.isPresent()) {
            for (KeyFieldMetadata keyFieldMetadata : pk.getMetadata().getKeyFields()) {
                SpeedyValue speedyValue = pk.get(keyFieldMetadata);
                Object value = JooqUtil.toJooqType(
                        speedyValue,
                        keyFieldMetadata.getColumnType()
                );
                Field<Object> field = JooqUtil.getColumn(keyFieldMetadata, dslContext.dialect());
                returnQuery.get().where(field.equal(value));
            }
            returnQuery.get().execute();
        }
        LOGGER.info("Update query: {}", returnQuery);
    }

    public void updateEntity(SpeedyEntityKey pk, SpeedyEntity entity) {
        dslContext.transaction(conf -> {
            updateEntityInTransaction(conf.dsl(), pk, entity);
        });
    }

    private boolean deleteEntity(DSLContext context, SpeedyEntityKey pk) throws SpeedyHttpException {
        EntityMetadata entityMetadata = pk.getMetadata();
        DeleteQuery<Record> deleteQuery = context.deleteQuery(DSL.table(entityMetadata.getDbTableName()));

        boolean isConditionProvided = false;

        // Add where conditions based on primary key fields
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            SpeedyValue speedyValue = pk.get(keyFieldMetadata);
            Object value = JooqUtil.toJooqType(
                    speedyValue,
                    keyFieldMetadata.getColumnType()
            );

            Field<Object> field = JooqUtil.getColumn(keyFieldMetadata, dslContext.dialect());

            deleteQuery.addConditions(field.equal(value));
            isConditionProvided = true;
        }
        LOGGER.info("Delete query: {}", deleteQuery);
        if (isConditionProvided) {
            deleteQuery.execute();
        }
        return true;
    }

    public void deleteEntity(List<SpeedyEntityKey> pks) {
        dslContext.transaction(conf -> {
            for (SpeedyEntityKey pk : pks) {
                deleteEntity(conf.dsl(), pk);
            }
        });
    }

}
