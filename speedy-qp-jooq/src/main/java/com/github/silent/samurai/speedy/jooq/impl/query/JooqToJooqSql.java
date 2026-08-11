package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import org.jooq.*;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/// Executes FK-based association expansion queries.
/// Used by {@link JooqBackend#selectByFks} (driven by the shared {@code RecordToSpeedy} walker)
/// during $expand resolution — one query per expansion level, not per row.
public class JooqToJooqSql {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqToJooqSql.class);

    private final DSLContext dslContext;

    public JooqToJooqSql(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    /// Fetches the related rows of {@code fieldMetadata}'s association whose key column matches any of
    /// {@code fkColumnValues} (the parent rows' already-converted foreign keys), as one {@code IN}
    /// query. Callers must pass a non-empty collection, already chunked to a size the backend accepts.
    public Result<Record> findByFKs(FieldMetadata fieldMetadata, Collection<?> fkColumnValues) {

        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
        FieldMetadata associationFieldMetadata = fieldMetadata.getAssociatedFieldMetadata();

        Field<Object> field = JooqUtil.getColumn(associationFieldMetadata, dslContext.dialect());
        Table<Record> table = JooqUtil.getTable(associationMetadata, dslContext.dialect());

        SelectConditionStep<Record> query = dslContext
                .select()
                .from(table)
                .where(field.in(fkColumnValues));

        LOGGER.debug("expand query: {} ", query);

        return query.fetch();
    }
}
