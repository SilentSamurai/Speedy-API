package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.interfaces.metadata.AssociationColumn;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/// Executes FK-based association expansion queries.
/// Used by {@link JooqBackend#selectByFks} (driven by the shared {@code RecordToSpeedy} walker)
/// during $expand resolution — one query per expansion level, not per row.
public class JooqToJooqSql {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqToJooqSql.class);

    private final DSLContext dslContext;

    public JooqToJooqSql(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    /// Fetches the related rows of {@code fieldMetadata}'s association whose key matches any of
    /// {@code fkRows} (the parent rows' already-converted foreign keys, each an ordered list holding
    /// one value per key column), as one {@code IN} query. A multi-column foreign key is matched with
    /// a row-value {@code IN}, which jOOQ emulates as an {@code OR} of {@code AND}s where the dialect
    /// has no native support. Callers must pass a non-empty collection, already chunked to a size the
    /// backend accepts.
    public Result<Record> findByFKs(FieldMetadata fieldMetadata, Collection<? extends List<?>> fkRows) {

        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
        List<AssociationColumn> associationColumns = fieldMetadata.getAssociationColumns();
        SQLDialect dialect = dslContext.dialect();

        Table<Record> table = JooqUtil.getTable(associationMetadata, dialect);

        Condition matchesAnyFk;
        if (associationColumns.size() == 1) {
            Field<Object> field = JooqUtil.getColumn(associationColumns.get(0).targetKeyField(), dialect);
            List<Object> values = new ArrayList<>(fkRows.size());
            for (List<?> row : fkRows) {
                values.add(row.get(0));
            }
            matchesAnyFk = field.in(values);
        } else {
            Field<?>[] keyColumns = associationColumns.stream()
                    .map(column -> JooqUtil.<Object>getColumn(column.targetKeyField(), dialect))
                    .toArray(Field[]::new);
            List<RowN> values = new ArrayList<>(fkRows.size());
            for (List<?> row : fkRows) {
                values.add(DSL.row(row.toArray()));
            }
            matchesAnyFk = DSL.row(keyColumns).in(values);
        }

        SelectConditionStep<Record> query = dslContext
                .select()
                .from(table)
                .where(matchesAnyFk);

        LOGGER.debug("expand query: {} ", query);

        return query.fetch();
    }
}
