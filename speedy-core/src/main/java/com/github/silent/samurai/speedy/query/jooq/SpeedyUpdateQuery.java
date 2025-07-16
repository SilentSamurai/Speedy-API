package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import org.jooq.*;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SpeedyUpdateQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyUpdateQuery.class);
    final SQLDialect dialect;
    private final DSLContext dslContext;

    public SpeedyUpdateQuery(DSLContext dslContext, SQLDialect dialect) {
        this.dslContext = dslContext;
        this.dialect = dialect;
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
        UpdateSetFirstStep<Record> updateQuery = dsl.update(JooqUtil.getTable(entityMetadata, dialect));
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
}
