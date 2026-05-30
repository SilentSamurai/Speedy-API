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

import java.util.ArrayList;
import java.util.List;

public class JooqPkQueryBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqPkQueryBuilder.class);
    final SQLDialect dialect;
    private final DSLContext dslContext;
    private final Converter converter;

    public JooqPkQueryBuilder(DSLContext dslContext, SQLDialect dialect, Converter converter) {
        this.dslContext = dslContext;
        this.dialect = dialect;
        this.converter = converter;
    }

    public Result<Record> findByPrimaryKey(SpeedyEntityKey pk) throws SpeedyHttpException {
        EntityMetadata entityMetadata = pk.getMetadata();
        SelectJoinStep<Record> query = dslContext.select()
                .from(JooqUtil.getTable(entityMetadata, dslContext.dialect()));

        // Build the where clause for each primary key field
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            SpeedyValue speedyValue = pk.get(keyFieldMetadata);
            Object value = converter.toColumnType(
                    speedyValue,
                    keyFieldMetadata
            );
            Field<Object> field = JooqUtil.getColumn(keyFieldMetadata, dslContext.dialect());

            query.where(field.eq(value));
        }

        LOGGER.info("Executing findByPrimaryKey query for entity '{}': {}", entityMetadata.getName(), query);
        return query.fetch();
    }

    public Result<Record> findByPrimaryKeys(List<SpeedyEntityKey> pks) throws SpeedyHttpException {
        if (pks == null || pks.isEmpty()) {
            return dslContext.newResult();
        }

        EntityMetadata entityMetadata = pks.get(0).getMetadata();
        SelectJoinStep<Record> query = dslContext.select()
                .from(JooqUtil.getTable(entityMetadata, dslContext.dialect()));

        List<KeyFieldMetadata> keyFields = new ArrayList<>(entityMetadata.getKeyFields());

        if (keyFields.size() == 1) {
            KeyFieldMetadata keyField = keyFields.get(0);
            List<Object> values = new ArrayList<>(pks.size());
            for (SpeedyEntityKey pk : pks) {
                values.add(converter.toColumnType(pk.get(keyField), keyField));
            }
            Field<Object> field = JooqUtil.getColumn(keyField, dslContext.dialect());
            query.where(field.in(values));
        } else {
            Condition combinedCondition = null;
            for (SpeedyEntityKey pk : pks) {
                Condition pkCondition = null;
                for (KeyFieldMetadata keyField : keyFields) {
                    Object value = converter.toColumnType(pk.get(keyField), keyField);
                    Field<Object> field = JooqUtil.getColumn(keyField, dslContext.dialect());
                    if (pkCondition == null) {
                        pkCondition = field.eq(value);
                    } else {
                        pkCondition = pkCondition.and(field.eq(value));
                    }
                }
                if (combinedCondition == null) {
                    combinedCondition = pkCondition;
                } else {
                    combinedCondition = combinedCondition.or(pkCondition);
                }
            }
            query.where(combinedCondition);
        }

        LOGGER.debug("Executing findByPrimaryKeys query for entity '{}': {}", entityMetadata.getName(), query);
        return query.fetch();
    }

}
