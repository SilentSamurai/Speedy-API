package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.Converter;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import org.jooq.*;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SpeedyDeleteQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyDeleteQuery.class);
    final SQLDialect dialect;
    private final DSLContext dslContext;
    private final Converter converter;
    
    public SpeedyDeleteQuery(DSLContext dslContext, SQLDialect dialect, Converter converter) {
        this.dslContext = dslContext;
        this.dialect = dialect;
        this.converter = converter;
    }

    private boolean deleteEntity(DSLContext context, SpeedyEntityKey pk) throws SpeedyHttpException {
        EntityMetadata entityMetadata = pk.getMetadata();
        DeleteQuery<Record> deleteQuery = context.deleteQuery(JooqUtil.getTable(entityMetadata, dialect));

        boolean isConditionProvided = false;

        // Add where conditions based on primary key fields
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            SpeedyValue speedyValue = pk.get(keyFieldMetadata);
            Object value = converter.toColumnType(
                    speedyValue,
                    keyFieldMetadata
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
