package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class JooqToJooqSql {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqToJooqSql.class);

    private final DSLContext dslContext;

    public JooqToJooqSql(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public Optional<Result<Record>> findByFK(FieldMetadata fieldMetadata,
                                             Record entityRecord) throws BadRequestException {

        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
        FieldMetadata associationFieldMetadata = fieldMetadata.getAssociatedFieldMetadata();

        Optional<?> optional = JooqUtil.getValueFromRecord(entityRecord, fieldMetadata);
        if (optional.isEmpty()) {
            LOGGER.error("foreign key not found: {}", fieldMetadata.getOutputPropertyName());
            return Optional.empty();
        }

        Field<Object> field = JooqUtil.getColumn(associationFieldMetadata);

        SelectConditionStep<Record> query = dslContext
                .select()
                .from(associationMetadata.getDbTableName())
                .where(field.eq(optional.get()));

        LOGGER.info("expand query: {} ", query);

        return Optional.of(query.fetch());
    }
}
