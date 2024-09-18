package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JooqToJooqSql {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqToJooqSql.class);

    private final DSLContext dslContext;

    public JooqToJooqSql(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public Result<Record> findByFK(FieldMetadata fieldMetadata,
                                   Record entityRecord) {

        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
        FieldMetadata associationFieldMetadata = fieldMetadata.getAssociatedFieldMetadata();

        Object value = JooqUtil.getValueFromRecord(entityRecord, fieldMetadata);

        Field<Object> field = JooqUtil.getColumn(associationFieldMetadata);

        SelectConditionStep<Record> query = dslContext
                .select()
                .from(associationMetadata.getDbTableName())
                .where(field.eq(value));

        LOGGER.info("expand query: {} ", query);

        return query.fetch();
    }
}
