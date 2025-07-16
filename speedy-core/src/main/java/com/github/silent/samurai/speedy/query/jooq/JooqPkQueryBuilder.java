package com.github.silent.samurai.speedy.query.jooq;


import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import org.jooq.*;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JooqPkQueryBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqPkQueryBuilder.class);
    final SQLDialect dialect;
    private final DSLContext dslContext;

    public JooqPkQueryBuilder(DSLContext dslContext, SQLDialect dialect) {
        this.dslContext = dslContext;
        this.dialect = dialect;
    }

    public Result<Record> findByPrimaryKey(SpeedyEntityKey pk) throws SpeedyHttpException {
        EntityMetadata entityMetadata = pk.getMetadata();
        SelectJoinStep<Record> query = dslContext.select()
                .from(JooqUtil.getTable(entityMetadata, dslContext.dialect()));

        // Build the where clause for each primary key field
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            SpeedyValue speedyValue = pk.get(keyFieldMetadata);
            Object value = JooqUtil.toJooqType(
                    speedyValue,
                    keyFieldMetadata.getColumnType()
            );
            Field<Object> field = JooqUtil.getColumn(keyFieldMetadata, dslContext.dialect());

            query.where(field.eq(value));
        }

        LOGGER.info("Executing findByPrimaryKey query for entity '{}': {}", entityMetadata.getName(), query);
        return query.fetch();
    }

}
