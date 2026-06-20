package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import org.jooq.*;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/// Executes FK-based association expansion queries.
/// Used by {@link JooqBackend#selectByFk} (driven by the shared {@code RecordToSpeedy} walker)
/// during $expand resolution.
public class JooqToJooqSql {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqToJooqSql.class);

    private final DSLContext dslContext;

    public JooqToJooqSql(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    /// Fetches the single related row of {@code fieldMetadata}'s association whose key column equals
    /// {@code fkColumnValue} (the parent row's already-converted foreign key); empty if none matches.
    public Optional<Record> findByFK(FieldMetadata fieldMetadata, Object fkColumnValue) {

        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
        FieldMetadata associationFieldMetadata = fieldMetadata.getAssociatedFieldMetadata();

        Field<Object> field = JooqUtil.getColumn(associationFieldMetadata, dslContext.dialect());

        SelectConditionStep<Record> query = dslContext
                .select()
                .from(associationMetadata.getDbTableName())
                .where(field.eq(fkColumnValue));

        LOGGER.info("expand query: {} ", query);

        Result<Record> result = query.fetch();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get(0));
    }
}
