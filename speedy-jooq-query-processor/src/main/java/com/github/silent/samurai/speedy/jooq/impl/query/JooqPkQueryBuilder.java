package com.github.silent.samurai.speedy.jooq.impl.query;


import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.jooq.impl.conversion.TypeConverter;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import org.jooq.*;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/// Builds and executes jOOQ queries for primary-key lookups.
/// Supports single-key IN optimised and composite-key OR fallback.
public class JooqPkQueryBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqPkQueryBuilder.class);
    final SQLDialect dialect;
    private final DSLContext dslContext;
    private final TypeConverter converter;

    public JooqPkQueryBuilder(DSLContext dslContext, SQLDialect dialect, TypeConverter converter) {
        this.dslContext = dslContext;
        this.dialect = dialect;
        this.converter = converter;
    }

    public Result<Record> findByPrimaryKey(SpeedyEntityKey pk) throws SpeedyHttpException {
        EntityMetadata entityMetadata = pk.getMetadata();
        SelectJoinStep<Record> query = dslContext.select()
                .from(JooqUtil.getTable(entityMetadata, dialect));
        query.where(pkCondition(pk));

        LOGGER.info("Executing findByPrimaryKey query for entity '{}': {}", entityMetadata.getName(), query);
        return query.fetch();
    }

    public Result<Record> findByPrimaryKeys(List<SpeedyEntityKey> pks) throws SpeedyHttpException {
        if (pks == null || pks.isEmpty()) {
            return dslContext.newResult();
        }

        EntityMetadata entityMetadata = pks.get(0).getMetadata();
        SelectJoinStep<Record> query = dslContext.select()
                .from(JooqUtil.getTable(entityMetadata, dialect));
        query.where(keysCondition(pks));

        LOGGER.debug("Executing findByPrimaryKeys query for entity '{}': {}", entityMetadata.getName(), query);
        return query.fetch();
    }

    /// {@code true} if a row exists for the given primary key, without selecting or decoding its
    /// columns (a {@code SELECT 1 ... WHERE <pk> ... LIMIT 1} probe).
    public boolean existsByPrimaryKey(SpeedyEntityKey pk) throws SpeedyHttpException {
        EntityMetadata entityMetadata = pk.getMetadata();
        return dslContext.fetchExists(JooqUtil.getTable(entityMetadata, dialect), pkCondition(pk));
    }

    /// AND of equality conditions across every key field of a single primary key.
    public Condition pkCondition(SpeedyEntityKey pk) throws SpeedyHttpException {
        Condition condition = null;
        for (KeyFieldMetadata keyField : pk.getMetadata().getKeyFields()) {
            Object value = converter.toColumnType(pk.get(keyField), keyField);
            Field<Object> field = JooqUtil.getColumn(keyField, dialect);
            Condition eq = field.eq(value);
            condition = (condition == null) ? eq : condition.and(eq);
        }
        return condition;
    }

    /// Condition matching any of the given primary keys: single-key {@code IN}, composite-key {@code OR}
    /// of per-key {@link #pkCondition}. Callers must pass a non-empty list.
    public Condition keysCondition(List<SpeedyEntityKey> pks) throws SpeedyHttpException {
        EntityMetadata entityMetadata = pks.get(0).getMetadata();
        List<KeyFieldMetadata> keyFields = new ArrayList<>(entityMetadata.getKeyFields());

        if (keyFields.size() == 1) {
            KeyFieldMetadata keyField = keyFields.get(0);
            List<Object> values = new ArrayList<>(pks.size());
            for (SpeedyEntityKey pk : pks) {
                values.add(converter.toColumnType(pk.get(keyField), keyField));
            }
            Field<Object> field = JooqUtil.getColumn(keyField, dialect);
            return field.in(values);
        }

        Condition combined = null;
        for (SpeedyEntityKey pk : pks) {
            Condition pkCondition = pkCondition(pk);
            combined = (combined == null) ? pkCondition : combined.or(pkCondition);
        }
        return combined;
    }

}
