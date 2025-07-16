package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import org.jooq.*;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SpeedyInsertQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyInsertQuery.class);
    final SQLDialect dialect;
    private final DSLContext dslContext;

    public SpeedyInsertQuery(DSLContext dslContext, SQLDialect dialect) {
        this.dslContext = dslContext;
        this.dialect = dialect;
    }

    public void insertEntity(List<SpeedyEntity> entities) {
        dslContext.transaction(conf -> {
            for (SpeedyEntity entity : entities) {
                insertEntity(conf.dsl(), entity);
            }
        });
    }

    public void insertEntity(DSLContext context, SpeedyEntity entity) throws SpeedyHttpException {
        EntityMetadata entityMetadata = entity.getMetadata();
        InsertSetStep<Record> insertQuery = context
                .insertInto(JooqUtil.getTable(entityMetadata, dialect));

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

}
