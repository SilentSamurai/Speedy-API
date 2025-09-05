package com.github.silent.samurai.speedy.query.jooq;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.data.MultipleFk;
import com.github.silent.samurai.speedy.data.Product;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.parser.JsonQueryParser;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
@Slf4j
@ExtendWith(MockitoExtension.class)
class JooqQueryBuilderTest {

    @Mock
    DataSource dataSource;

    @Mock
    MetaModel metaModel;

    EntityMetadata productMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
    EntityMetadata fkMetadata = StaticEntityMetadata.createEntityMetadata(MultipleFk.class);

    DSLContext dslContext;

    @BeforeEach
    void setUp() throws NotFoundException {
        dslContext = DSL.using(dataSource, SQLDialect.H2);
        Mockito.lenient().when(metaModel.findEntityMetadata("Product")).thenReturn(productMetadata);
        Mockito.lenient().when(metaModel.findEntityMetadata("MultipleFk")).thenReturn(fkMetadata);
    }

    String json2SqlQuery(JsonNode jsonQuery) throws SpeedyHttpException {
        EntityMetadata entityMetadata = metaModel.findEntityMetadata(jsonQuery.get("$from").asText());
        JsonQueryParser builder = new JsonQueryParser(metaModel, entityMetadata, jsonQuery);
        JooqQueryBuilder qb = new JooqQueryBuilder(builder.build(), dslContext, new JooqConversionImpl());
        qb.prepareQuery();
        String sql = qb.query.toString();
        log.info("sql: {}", sql);
        return sql;
    }

    @Test
    void executeQuery() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("Product")
                .where(
                        condition("id", eq("1"))
                )
                .prettyPrint()
                .build();

        String actualSql = json2SqlQuery(jsonQuery);
        String expectedQuery = """
                select *
                from "PRODUCT"
                where "PRODUCT"."ID" = '1'
                offset 0 rows
                fetch next 10 rows only""";
        assertEquals(expectedQuery, actualSql);
    }

    @Test
    void join() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("MultipleFk")
                .where(
                        and(
                                condition("a.id", eq("1")),
                                condition("b.id", eq("2"))
                        )
                )
                .prettyPrint()
                .build();

        String expectedSql = """
                select *
                from "MULTIPLEFK"
                  join "PRODUCT" "Product_1"
                    on "MULTIPLEFK"."A" = "Product_1"."ID"
                  join "PRODUCT" "Product_2"
                    on "MULTIPLEFK"."B" = "Product_2"."ID"
                where (
                  "Product_1"."ID" = '1'
                  and "Product_2"."ID" = '2'
                )
                offset 0 rows
                fetch next 10 rows only""";
        assertEquals(expectedSql, json2SqlQuery(jsonQuery));

    }

    @Test
    void join_2() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("MultipleFk")
                .where(
                        and(
                                condition("a.name", eq("product-a")),
                                condition("b.name", eq("product-b"))
                        )
                )
                .prettyPrint()
                .build();

        String expected = """
                select *
                from "MULTIPLEFK"
                  join "PRODUCT" "Product_1"
                    on "MULTIPLEFK"."A" = "Product_1"."ID"
                  join "PRODUCT" "Product_2"
                    on "MULTIPLEFK"."B" = "Product_2"."ID"
                where (
                  "Product_1"."NAME" = 'product-a'
                  and "Product_2"."NAME" = 'product-b'
                )
                offset 0 rows
                fetch next 10 rows only""";
        assertEquals(expected, json2SqlQuery(jsonQuery));
    }

    @Test
    void where_eq() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("MultipleFk")
                .where(
                        condition("id", eq("1"))
                )
                .prettyPrint()
                .build();

        String expected = """
                select *
                from "MULTIPLEFK"
                where "MULTIPLEFK"."ID" = '1'
                offset 0 rows
                fetch next 10 rows only""";
        assertEquals(expected, json2SqlQuery(jsonQuery));
    }

    @Test
    void where_neq() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("MultipleFk")
                .where(
                        condition("id", ne("1"))
                )
                .prettyPrint()
                .build();

        String expected = """
                select *
                from "MULTIPLEFK"
                where "MULTIPLEFK"."ID" <> '1'
                offset 0 rows
                fetch next 10 rows only""";

        assertEquals(expected, json2SqlQuery(jsonQuery));
    }

    @Test
    void where_gt() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("Product")
                .where(
                        condition("cost", gt("100"))
                )
                .prettyPrint()
                .build();

        String expected = """
                select *
                from "PRODUCT"
                where "PRODUCT"."COST" > 100
                offset 0 rows
                fetch next 10 rows only""";

        assertEquals(expected, json2SqlQuery(jsonQuery));
    }

    @Test
    void where_gte() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("Product")
                .where(
                        condition("cost", gte("100"))
                )
                .prettyPrint()
                .build();

        String expected = """
                select *
                from "PRODUCT"
                where "PRODUCT"."COST" >= 100
                offset 0 rows
                fetch next 10 rows only""";

        assertEquals(expected, json2SqlQuery(jsonQuery));
    }

    @Test
    void where_lt() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("Product")
                .where(
                        condition("cost", lt("100"))
                )
                .prettyPrint()
                .build();

        String expected = """
                select *
                from "PRODUCT"
                where "PRODUCT"."COST" < 100
                offset 0 rows
                fetch next 10 rows only""";

        assertEquals(expected, json2SqlQuery(jsonQuery));
    }

    @Test
    void where_lte() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("Product")
                .where(
                        condition("cost", lte("100"))
                )
                .prettyPrint()
                .build();

        String expected = """
                select *
                from "PRODUCT"
                where "PRODUCT"."COST" <= 100
                offset 0 rows
                fetch next 10 rows only""";

        assertEquals(expected, json2SqlQuery(jsonQuery));
    }

    @Test
    void where_like() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("Product")
                .where(
                        condition("name", matches("P*1"))
                )
                .prettyPrint()
                .build();

        String expected = """
                select *
                from "PRODUCT"
                where "PRODUCT"."NAME" like 'P%1' escape '\\'
                offset 0 rows
                fetch next 10 rows only""";

        assertEquals(expected, json2SqlQuery(jsonQuery));
    }

    @Test
    void where_like_with_escape() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("Product")
                .where(
                        condition("name", matches("P1%"))
                )
                .prettyPrint()
                .build();

        String expected = """
                select *
                from "PRODUCT"
                where "PRODUCT"."NAME" like 'P1\\%' escape '\\'
                offset 0 rows
                fetch next 10 rows only""";

        assertEquals(expected, json2SqlQuery(jsonQuery));
    }

    @Test
    void where_in() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("MultipleFk")
                .where(
                        condition("category", in("A", "B", "C"))
                )
                .prettyPrint()
                .build();

        String expected = """
                select *
                from "MULTIPLEFK"
                where "MULTIPLEFK"."CATEGORY" in (
                  'A', 'B', 'C'
                )
                offset 0 rows
                fetch next 10 rows only""";

        assertEquals(expected, json2SqlQuery(jsonQuery));
    }

    @Test
    void where_not_in() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("MultipleFk")
                .where(
                        condition("category", nin("A", "B", "C"))
                )
                .prettyPrint()
                .build();

        String expected = """
                select *
                from "MULTIPLEFK"
                where "MULTIPLEFK"."CATEGORY" not in (
                  'A', 'B', 'C'
                )
                offset 0 rows
                fetch next 10 rows only""";

        assertEquals(expected, json2SqlQuery(jsonQuery));
    }

    @Test
    void where_is_null() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("MultipleFk")
                .where(
                        condition("a", eq(null))
                )
                .prettyPrint()
                .build();

        String expected = """
                select *
                from "MULTIPLEFK"
                where "MULTIPLEFK"."A" is null
                offset 0 rows
                fetch next 10 rows only""";

        assertEquals(expected, json2SqlQuery(jsonQuery));
    }

    @Test
    void where_is_not_null() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("MultipleFk")
                .where(
                        condition("a", ne(null))
                )
                .prettyPrint()
                .build();

        String expected = """
                select *
                from "MULTIPLEFK"
                where "MULTIPLEFK"."A" is not null
                offset 0 rows
                fetch next 10 rows only""";

        assertEquals(expected, json2SqlQuery(jsonQuery));
    }

    @Test
    void where_and_conditions() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("MultipleFk")
                .where(
                        and(
                                condition("id", eq("1")),
                                condition("category", ne("X"))
                        )
                )
                .prettyPrint()
                .build();

        String sqlQuery = json2SqlQuery(jsonQuery);

        String query = """
                select *
                from "MULTIPLEFK"
                where (
                  "MULTIPLEFK"."ID" = '1'
                  and "MULTIPLEFK"."CATEGORY" <> 'X'
                )
                offset 0 rows
                fetch next 10 rows only""";
        assertEquals(query, sqlQuery);
    }

    @Test
    void where_or_conditions() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("MultipleFk")
                .where(
                        or(
                                condition("id", eq("1")),
                                condition("category", eq("A"))
                        )
                )
                .prettyPrint()
                .build();

        String json2SqlQuery = json2SqlQuery(jsonQuery);

        String sqlQuery = """
                select *
                from "MULTIPLEFK"
                where (
                  "MULTIPLEFK"."ID" = '1'
                  or "MULTIPLEFK"."CATEGORY" = 'A'
                )
                offset 0 rows
                fetch next 10 rows only""";
        assertEquals(sqlQuery, json2SqlQuery);
    }

    @Test
    void where_complex_nested_conditions() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("Product")
                .where(
                        and(
                                or(
                                        condition("id", eq("1")),
                                        condition("category", eq("A"))
                                ),
                                condition("cost", gt("100"))
                        )
                )
                .prettyPrint()
                .build();

        String sql = json2SqlQuery(jsonQuery);

        String query = """
                select *
                from "PRODUCT"
                where (
                  (
                    "PRODUCT"."ID" = '1'
                    or "PRODUCT"."CATEGORY" = 'A'
                  )
                  and "PRODUCT"."COST" > 100
                )
                offset 0 rows
                fetch next 10 rows only""";
        assertEquals(query, sql);
    }

    @Test
    void where_complex_deep_nested_conditions() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("Product")
                .where(
                        and(
                                or(
                                        condition("id", eq("1")),
                                        condition("category", eq("A")),
                                        and(
                                                condition("id", eq("1")),
                                                condition("category", eq("A"))
                                        )
                                ),
                                condition("cost", gt("100")),
                                and(
                                        condition("cost", gt("100")),
                                        condition("cost", gt("100")),
                                        or(
                                                condition("id", eq("1")),
                                                condition("category", eq("A"))
                                        )
                                )
                        )
                )
                .prettyPrint()
                .build();

        String actualQuery = json2SqlQuery(jsonQuery);

        String expectedQuery = """
                select *
                from "PRODUCT"
                where (
                  (
                    "PRODUCT"."ID" = '1'
                    or "PRODUCT"."CATEGORY" = 'A'
                    or (
                      "PRODUCT"."ID" = '1'
                      and "PRODUCT"."CATEGORY" = 'A'
                    )
                  )
                  and "PRODUCT"."COST" > 100
                  and "PRODUCT"."COST" > 100
                  and "PRODUCT"."COST" > 100
                  and (
                    "PRODUCT"."ID" = '1'
                    or "PRODUCT"."CATEGORY" = 'A'
                  )
                )
                offset 0 rows
                fetch next 10 rows only""";

        assertEquals(expectedQuery, actualQuery);
    }

    @Test
    void order_by() throws Exception {
        JsonNode jsonQuery = SpeedyQuery.from()
                .fromEntity("MultipleFk")
                .orderByAsc("name")
                .orderByDesc("id")
                .prettyPrint()
                .build();

        String json2SqlQuery = json2SqlQuery(jsonQuery);

        String sqlQuery = """
                select *
                from "MULTIPLEFK"
                order by "MULTIPLEFK"."NAME" asc, "MULTIPLEFK"."ID" desc
                offset 0 rows
                fetch next 10 rows only""";
        assertEquals(sqlQuery, json2SqlQuery);
    }
}