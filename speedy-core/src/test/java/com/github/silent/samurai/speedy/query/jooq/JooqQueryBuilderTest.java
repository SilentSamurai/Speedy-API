package com.github.silent.samurai.speedy.query.jooq;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.data.Product;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.query.Json2SpeedyQueryBuilder;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;

@Slf4j
@ExtendWith(MockitoExtension.class)
class JooqQueryBuilderTest {

    @Mock
    DataSource dataSource;

    @Mock
    MetaModel metaModel;

    EntityMetadata productMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);

    DSLContext dslContext;

    @BeforeEach
    void setUp() throws NotFoundException {
        dslContext = DSL.using(dataSource, SQLDialect.H2);
//        Mockito.when(metaModelProcessor.findEntityMetadata("Product")).thenReturn(productMetadata);
    }

    @Test
    void executeQuery() throws Exception {
        ObjectNode jsonQuery = CommonUtil.json()
                .createObjectNode()
                .putObject("$where")
                .putObject("productItem")
                .put("id", "1");

        Json2SpeedyQueryBuilder builder = new Json2SpeedyQueryBuilder(metaModel, productMetadata, jsonQuery);
        SpeedyQuery speedyQuery = builder.build();

        JooqQueryBuilder qb = new JooqQueryBuilder(speedyQuery, dslContext);

        qb.prepareQuery();

        String sql = qb.query.getSQL();

        log.info("sql: {}", sql);

    }
}