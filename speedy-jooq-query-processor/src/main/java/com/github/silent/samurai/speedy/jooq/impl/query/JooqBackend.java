package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.query.backend.SpeedyBackend;
import com.github.silent.samurai.speedy.jooq.impl.conversion.Converter;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import org.jooq.DSLContext;
import org.jooq.DeleteQuery;
import org.jooq.Field;
import org.jooq.InsertSetMoreStep;
import org.jooq.InsertSetStep;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.UpdateSetFirstStep;
import org.jooq.UpdateSetMoreStep;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/// jOOQ implementation of the {@link SpeedyBackend} port. Owns the {@link DSLContext} and the
/// transactional context, and translates the shared walker's row/column operations into jOOQ
/// statements. This is the jOOQ-specific code that the format-agnostic
/// {@code WalkingQueryProcessor} drives — the persistence analogue of {@code JsonResponseWriter}.
public class JooqBackend implements SpeedyBackend {

    private final SQLDialect dialect;
    private final Settings settings = new Settings()
            .withRenderQuotedNames(RenderQuotedNames.ALWAYS)
            .withRenderNameStyle(RenderNameStyle.AS_IS);

    private final Converter converter;
    private final DSLContext dslContext;
    private final ThreadLocal<DSLContext> transactionalDslContext = new ThreadLocal<>();

    public JooqBackend(DataSource dataSource, SpeedyDialect speedyDialect, Converter converter) {
        this.dialect = JooqUtil.toJooqDialect(speedyDialect);
        this.dslContext = DSL.using(dataSource, dialect, settings);
        this.converter = converter;
    }

    private DSLContext dsl() {
        DSLContext tx = transactionalDslContext.get();
        return tx != null ? tx : dslContext;
    }

    // ---- RowReader ----

    @Override
    public List<SpeedyEntity> select(SpeedyQuery query) throws SpeedyHttpException {
        DSLContext dsl = dsl();
        JooqQueryBuilder qb = new JooqQueryBuilder(query, dsl, converter);
        Result<? extends Record> result = qb.executeQuery();
        return wrap(result, query.getFrom());
    }

    @Override
    public BigInteger count(SpeedyQuery query) throws SpeedyHttpException {
        JooqQueryBuilder qb = new JooqQueryBuilder(query, dsl(), converter);
        return qb.executeCountQuery();
    }

    @Override
    public List<SpeedyEntity> selectByKeys(List<SpeedyEntityKey> keys) throws SpeedyHttpException {
        if (keys.isEmpty()) {
            return List.of();
        }
        Result<Record> result = new JooqPkQueryBuilder(dsl(), dialect, converter).findByPrimaryKeys(keys);
        return wrap(result, keys.get(0).getMetadata());
    }

    @Override
    public Optional<SpeedyEntity> selectByFk(FieldMetadata association, SpeedyEntity parentRow) throws SpeedyHttpException {
        if (!parentRow.has(association)) {
            return Optional.empty();
        }
        SpeedyValue fk = parentRow.get(association);
        if (fk.isNull()) {
            return Optional.empty();
        }
        // The FK is stored under the association field; re-encode it (with the associated field's
        // type) to query the related table.
        Object fkColumnValue = converter.toColumnType(fk, association.getAssociatedFieldMetadata());
        Optional<Record> associatedRecord = new JooqToJooqSql(dsl()).findByFK(association, fkColumnValue);
        if (associatedRecord.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toFlatEntity(associatedRecord.get(), association.getAssociationMetadata()));
    }

    // ---- RowWriter ----

    @Override
    public void insert(SpeedyEntity columns) throws SpeedyHttpException {
        EntityMetadata entityMetadata = columns.getMetadata();
        InsertSetStep<Record> insertQuery = dsl().insertInto(JooqUtil.getTable(entityMetadata, dialect));
        InsertSetMoreStep<Record> step = null;
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!columns.has(fieldMetadata)) {
                continue;
            }
            Field<Object> field = JooqUtil.getColumn(fieldMetadata, dialect);
            Object value = toColumnValue(fieldMetadata, columns.get(fieldMetadata));
            step = (step == null ? insertQuery.set(field, value) : step.set(field, value));
        }
        if (step == null) {
            return;
        }
        java.util.List<KeyFieldMetadata> generatedKeys = new java.util.ArrayList<>();
        for (KeyFieldMetadata keyField : entityMetadata.getKeyFields()) {
            if (!keyField.isInsertable()) {
                generatedKeys.add(keyField);
            }
        }
        if (generatedKeys.isEmpty()) {
            step.execute();
            return;
        }
        java.util.List<Field<?>> returningFields = new java.util.ArrayList<>();
        for (KeyFieldMetadata kf : generatedKeys) {
            returningFields.add(JooqUtil.getColumn(kf, dialect));
        }
        Record result = step.returning(returningFields.toArray(new Field[0])).fetchOne();
        if (result != null) {
            for (KeyFieldMetadata kf : generatedKeys) {
                FieldMetadata conversionField = JooqUtil.conversionField(kf);
                String columnName = JooqUtil.transformIdentifier(kf.getDbColumnName(), dialect);
                Object value = result.get(columnName, Object.class);
                if (value != null) {
                    columns.put(kf, converter.toSpeedyValue(value, conversionField));
                }
            }
        }
    }

    @Override
    public void update(SpeedyEntityKey pk, SpeedyEntity columns) throws SpeedyHttpException {
        EntityMetadata entityMetadata = columns.getMetadata();
        UpdateSetFirstStep<Record> updateQuery = dsl().update(JooqUtil.getTable(entityMetadata, dialect));
        UpdateSetMoreStep<Record> step = null;
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!columns.has(fieldMetadata)) {
                continue;
            }
            Field<Object> field = JooqUtil.getColumn(fieldMetadata, dialect);
            Object value = toColumnValue(fieldMetadata, columns.get(fieldMetadata));
            step = (step == null ? updateQuery.set(field, value) : step.set(field, value));
        }
        if (step == null) {
            return;
        }
        for (KeyFieldMetadata keyFieldMetadata : pk.getMetadata().getKeyFields()) {
            Object value = converter.toColumnType(pk.get(keyFieldMetadata), keyFieldMetadata);
            Field<Object> field = JooqUtil.getColumn(keyFieldMetadata, dialect);
            step.where(field.equal(value));
        }
        step.execute();
    }

    @Override
    public void delete(SpeedyEntityKey pk) throws SpeedyHttpException {
        EntityMetadata entityMetadata = pk.getMetadata();
        DeleteQuery<Record> deleteQuery = dsl().deleteQuery(JooqUtil.getTable(entityMetadata, dialect));
        boolean conditionProvided = false;
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            Object value = converter.toColumnType(pk.get(keyFieldMetadata), keyFieldMetadata);
            Field<Object> field = JooqUtil.getColumn(keyFieldMetadata, dialect);
            deleteQuery.addConditions(field.equal(value));
            conditionProvided = true;
        }
        if (conditionProvided) {
            deleteQuery.execute();
        }
    }

    // ---- BackendSession ----

    @Override
    public void runInTransaction(Runnable block) {
        dslContext.transaction(conf -> {
            transactionalDslContext.set(conf.dsl());
            try {
                block.run();
            } finally {
                transactionalDslContext.remove();
            }
        });
    }

    @Override
    public Optional<SpeedyHttpException> classify(Exception cause) {
        if (cause instanceof org.jooq.exception.DataAccessException dae) {
            Throwable sqlCause = dae.getCause();
            if (sqlCause instanceof java.sql.SQLException sqle) {
                String state = sqle.getSQLState();
                if (state != null && (state.startsWith("23") || state.startsWith("22"))) {
                    return Optional.of(new BadRequestException("Invalid Request", dae));
                }
            }
        }
        return Optional.empty();
    }

    /// Converts a column's {@link SpeedyValue} to its JDBC column type. For an association field the
    /// stored value is the foreign key, whose type is described by the associated field's metadata —
    /// the same special-case {@link JooqUtil#getColumn} applies when typing the FK column.
    private Object toColumnValue(FieldMetadata field, SpeedyValue value) {
        return converter.toColumnType(value, JooqUtil.conversionField(field));
    }

    private List<SpeedyEntity> wrap(Result<? extends Record> result, EntityMetadata entityMetadata) throws SpeedyHttpException {
        List<SpeedyEntity> rows = new ArrayList<>(result.size());
        for (Record record : result) {
            rows.add(toFlatEntity(record, entityMetadata));
        }
        return rows;
    }

    /// Decodes a jOOQ {@link Record} into a *flat* {@link SpeedyEntity}: every readable column becomes
    /// a {@link SpeedyValue} (this is where value decoding — the leaf codec — lives), with each
    /// association field carrying its foreign-key value (decoded with the associated field's type).
    /// Columns absent from the record (not selected, or SQL {@code NULL}) are left unset; the shared
    /// {@code RecordToSpeedy} walker then resolves associations into nested entities or keys.
    private SpeedyEntity toFlatEntity(Record record, EntityMetadata entityMetadata) throws SpeedyHttpException {
        SpeedyEntity row = new SpeedyEntity(entityMetadata);
        for (FieldMetadata field : entityMetadata.getAllFields()) {
            Optional<Object> raw = JooqUtil.getValueFromRecord(record, field, dialect);
            if (raw.isEmpty()) {
                continue;
            }
            Object value = raw.get();
            // For an association field the stored value is the foreign key, decoded with the associated
            // field's type — the same special-case JooqUtil.getColumn applies when typing the FK column.
            FieldMetadata conversionField = JooqUtil.conversionField(field);
            if (field.isCollection() && !field.isAssociation()) {
                // jOOQ sometimes promotes scalar types (int -> decimal and vice-versa); the converter normalises.
                Collection<?> items = (Collection<?>) value;
                List<SpeedyValue> list = new LinkedList<>();
                for (Object item : items) {
                    list.add(converter.toSpeedyValue(item, conversionField));
                }
                row.put(field, new SpeedyCollection(list));
            } else {
                row.put(field, converter.toSpeedyValue(value, conversionField));
            }
        }
        return row;
    }
}
