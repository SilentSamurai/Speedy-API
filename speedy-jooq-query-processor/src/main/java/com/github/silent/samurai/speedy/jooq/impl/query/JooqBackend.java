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
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/// jOOQ implementation of the {@link SpeedyBackend} port. Owns the {@link DSLContext} and the
/// transactional context, and translates the shared walker's row/column operations into jOOQ
/// statements. This is the jOOQ-specific code that the format-agnostic
/// {@code DefaultQueryProcessor} drives — the persistence analogue of {@code JsonResponseWriter}.
public class JooqBackend implements SpeedyBackend {

    // MySQL/MariaDB error codes surfaced under the generic HY000 SQLState (H2/Postgres use 22/23).
    private static final int ER_NO_DEFAULT_FOR_FIELD = 1364;
    private static final int ER_TRUNCATED_WRONG_VALUE_FOR_FIELD = 1366;

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
    public boolean existsByKey(SpeedyEntityKey key) throws SpeedyHttpException {
        return new JooqPkQueryBuilder(dsl(), dialect, converter).existsByPrimaryKey(key);
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
    public void insert(List<SpeedyEntity> entities) throws SpeedyHttpException {
        List<InsertSetMoreStep<Record>> batch = new ArrayList<>(entities.size());
        for (SpeedyEntity columns : entities) {
            InsertSetMoreStep<Record> step = buildInsertStep(columns);
            if (step == null) {
                // entity has no insertable columns
                continue;
            }
            List<KeyFieldMetadata> generatedKeys = generatedKeysToReadBack(columns);
            if (generatedKeys.isEmpty()) {
                // No DB-assigned key to read back, so it can ride along in a single batched round-trip.
                batch.add(step);
            } else {
                // Must read the generated key back onto this entity, which batch execution can't do.
                populateGeneratedKeys(step, columns, generatedKeys);
            }
        }
        if (!batch.isEmpty()) {
            dsl().batch(batch).execute();
        }
    }

    /// Builds the INSERT step for one entity's set columns, or {@code null} if it has none.
    private InsertSetMoreStep<Record> buildInsertStep(SpeedyEntity columns) {
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
        return step;
    }

    /// Keys the database assigns that the entity doesn't already carry. App-generated keys (e.g. UUID)
    /// are produced before insert and written as a normal column, so they need no read-back — only
    /// DB-assigned keys (IDENTITY / AUTO_INCREMENT) do. Skipping already-present keys also avoids
    /// asking jOOQ to "return" a non-identity column, which MySQL's getGeneratedKeys emulation rejects
    /// with "Field '<pk>' does not exist".
    private List<KeyFieldMetadata> generatedKeysToReadBack(SpeedyEntity columns) {
        List<KeyFieldMetadata> generatedKeys = new ArrayList<>();
        for (KeyFieldMetadata keyField : columns.getMetadata().getKeyFields()) {
            if (!keyField.isInsertable() && !columns.has(keyField)) {
                generatedKeys.add(keyField);
            }
        }
        return generatedKeys;
    }

    /// Executes the insert and writes the database-assigned key(s) back onto {@code columns} so the
    /// caller can refetch the row by primary key. Postgres/H2 return the values via a native
    /// {@code RETURNING} clause. MySQL/MariaDB have no {@code RETURNING}: jOOQ would fall back to
    /// JDBC {@code getGeneratedKeys()}, which surfaces the value under a synthetic label (e.g.
    /// {@code GENERATED_KEY}) instead of the real column name, so a single identity is read with the
    /// canonical {@code LAST_INSERT_ID()} idiom instead.
    private void populateGeneratedKeys(InsertSetMoreStep<Record> step, SpeedyEntity columns,
                                       List<KeyFieldMetadata> generatedKeys) throws SpeedyHttpException {
        if (generatedKeys.size() == 1 && !supportsReturning()) {
            step.execute();
            KeyFieldMetadata kf = generatedKeys.get(0);
            Object value = dsl().lastID();
            if (value != null) {
                columns.put(kf, converter.toSpeedyValue(value, JooqUtil.conversionField(kf)));
            }
            return;
        }
        List<Field<?>> returningFields = new ArrayList<>(generatedKeys.size());
        for (KeyFieldMetadata kf : generatedKeys) {
            returningFields.add(JooqUtil.getColumn(kf, dialect));
        }
        Record result = step.returning(returningFields.toArray(new Field[0])).fetchOne();
        if (result == null) {
            return;
        }
        for (int i = 0; i < generatedKeys.size(); i++) {
            KeyFieldMetadata kf = generatedKeys.get(i);
            Object value = returnedValue(result, returningFields.get(i), i);
            if (value != null) {
                columns.put(kf, converter.toSpeedyValue(value, JooqUtil.conversionField(kf)));
            }
        }
    }

    /// Reads a generated-key value from a {@code RETURNING} record: by the requested field first,
    /// then positionally to tolerate dialects that surface generated keys under a synthetic label.
    private static Object returnedValue(Record result, Field<?> field, int index) {
        if (result.field(field) != null) {
            return result.get(field);
        }
        if (index < result.size()) {
            return result.get(index);
        }
        return null;
    }

    /// Whether the dialect supports a SQL {@code RETURNING}/{@code OUTPUT} clause for INSERT. MySQL
    /// and MariaDB don't, so generated keys are read via {@code LAST_INSERT_ID()} instead.
    private boolean supportsReturning() {
        return dialect != SQLDialect.MYSQL && dialect != SQLDialect.MARIADB;
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
    public void deleteByKeys(List<SpeedyEntityKey> keys) throws SpeedyHttpException {
        if (keys.isEmpty()) {
            return;
        }
        EntityMetadata entityMetadata = keys.get(0).getMetadata();
        // No key fields means no safe WHERE — never emit an unconditional DELETE.
        if (entityMetadata.getKeyFields().isEmpty()) {
            return;
        }
        DeleteQuery<Record> deleteQuery = dsl().deleteQuery(JooqUtil.getTable(entityMetadata, dialect));
        deleteQuery.addConditions(new JooqPkQueryBuilder(dsl(), dialect, converter).keysCondition(keys));
        deleteQuery.execute();
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
        if (cause instanceof DataAccessException dae) {
            Throwable sqlCause = dae.getCause();
            if (sqlCause instanceof SQLException sqle) {
                String state = sqle.getSQLState();
                // 23xxx = integrity-constraint violation, 22xxx = data exception (bad client input).
                if (state != null && (state.startsWith("23") || state.startsWith("22"))) {
                    return Optional.of(new BadRequestException("Invalid Request", dae));
                }
                // MySQL/MariaDB report a missing required column (no default) or a wrong-typed value
                // under the generic HY000 state, where H2/Postgres use 22/23. These are still bad
                // client input, so normalise them to 400 too.
                int errorCode = sqle.getErrorCode();
                if (errorCode == ER_NO_DEFAULT_FOR_FIELD || errorCode == ER_TRUNCATED_WRONG_VALUE_FOR_FIELD) {
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
        Object columnValue = converter.toColumnType(value, JooqUtil.conversionField(field));
        return JooqUtil.toDialectColumnValue(columnValue, dialect);
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
