package com.github.silent.samurai.speedy.serialization;

import com.github.silent.samurai.speedy.data.Product;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.StructureReader;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Identifier;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/// Exercises the streaming {@link StructureToQuery} walker over an {@link InMemoryStructureReader}
/// (no format module). The JSON strings are only a convenient way to build the input token tree.
class StructureToQueryTest {

    private EntityMetadata productMetadata;

    @BeforeEach
    void setUp() {
        productMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
    }

    private SpeedyQuery build(EntityMetadata entity, String json) throws SpeedyHttpException {
        return build(entity, json, Integer.MAX_VALUE, 20);
    }

    private SpeedyQuery build(EntityMetadata entity, String json, int maxPageSize, int defaultPageSize)
            throws SpeedyHttpException {
        StructureReader reader = new InMemoryStructureReader(readTree(json));
        return new StructureToQuery().parse(entity, reader, maxPageSize, defaultPageSize);
    }

    private Object readTree(String json) throws SpeedyHttpException {
        try {
            return CommonUtil.json().readValue(json, Object.class);
        } catch (IOException e) {
            throw new BadRequestException("invalid test json", e);
        }
    }

    @Test
    void query_with_simple_conditions() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$where": {
                        "id": "abcd-efgh",
                        "cost": {
                            "$eq": "0"
                        }
                    }
                }
                """;

        SpeedyQuery query = build(productMetadata, json);

        assertNotNull(query);
        BooleanCondition whereCondition = query.getWhere();
        assertNotNull(whereCondition.getConditions());
        assertEquals(2, whereCondition.getConditions().size());

        BinaryCondition idCondition = (BinaryCondition) whereCondition.getConditions().get(0);
        assertEquals(ConditionOperator.EQ, idCondition.getOperator());
        assertEquals("id", idCondition.getField().getFieldMetadata().getOutputPropertyName());

        BinaryCondition costCondition = (BinaryCondition) whereCondition.getConditions().get(1);
        assertEquals(ConditionOperator.EQ, costCondition.getOperator());
        assertEquals("cost", costCondition.getField().getFieldMetadata().getOutputPropertyName());
    }

    @Test
    void query_with_invalid_where_type() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$where": "invalidWhere"
                }
                """;

        assertThrows(BadRequestException.class, () -> build(productMetadata, json));
    }

    @Test
    void query_with_valid_order_by_clause() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$orderBy": {
                        "id": "ASC",
                        "name": "DESC"
                    }
                }
                """;

        SpeedyQuery query = build(productMetadata, json);

        assertNotNull(query);
        Set<String> collect = query.getOrderByList().stream()
                .map(by -> by.getFieldMetadata().getOutputPropertyName())
                .collect(Collectors.toSet());
        assertTrue(collect.contains("id"));
        assertTrue(collect.contains("name"));
    }

    @Test
    void query_with_invalid_order_direction() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$orderBy": {
                        "id": "INVALID_ORDER"
                    }
                }
                """;

        assertThrows(BadRequestException.class, () -> build(productMetadata, json));
    }

    @Test
    void query_with_valid_page_index_and_size() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$page": {
                        "$index": 1,
                        "$size": 20
                    }
                }
                """;

        SpeedyQuery query = build(productMetadata, json);

        assertNotNull(query);
        assertEquals(1, query.getPageInfo().getPageNo());
        assertEquals(20, query.getPageInfo().getPageSize());
    }

    @Test
    void query_with_missing_page_index() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$page": {
                        "$size": 20
                    }
                }
                """;

        SpeedyQuery query = build(productMetadata, json);

        assertNotNull(query);
        assertEquals(0, query.getPageInfo().getPageNo());  // Default value
        assertEquals(20, query.getPageInfo().getPageSize());
    }

    @Test
    void query_with_valid_expand_list() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$expand": ["relation"]
                }
                """;

        SpeedyQuery query = build(productMetadata, json);

        assertNotNull(query);
        assertTrue(query.getExpand().contains("relation"));
    }

    @Test
    void query_with_invalid_from_type() throws Exception {
        String json = """
                {
                    "$from": 12345
                }
                """;

        assertThrows(BadRequestException.class, () -> build(productMetadata, json));
    }

    @Test
    void query_with_field_references() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$where": {
                        "id": "$name",
                        "cost": {
                            "$lt": "$category"
                        }
                    }
                }
                """;

        SpeedyQuery query = build(productMetadata, json);

        BinaryCondition idCondition = (BinaryCondition) query.getWhere().getConditions().get(0);
        assertEquals(ConditionOperator.EQ, idCondition.getOperator());
        assertEquals("id", idCondition.getField().getFieldMetadata().getOutputPropertyName());
        assertInstanceOf(Identifier.class, idCondition.getExpression());
        assertEquals("name", ((Identifier) idCondition.getExpression()).field().getFieldMetadata().getOutputPropertyName());

        BinaryCondition costCondition = (BinaryCondition) query.getWhere().getConditions().get(1);
        assertEquals(ConditionOperator.LT, costCondition.getOperator());
        assertEquals("cost", costCondition.getField().getFieldMetadata().getOutputPropertyName());
        assertInstanceOf(Identifier.class, costCondition.getExpression());
        assertEquals("category", ((Identifier) costCondition.getExpression()).field().getFieldMetadata().getOutputPropertyName());
    }

    @Test
    void query_with_invalid_field_reference() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$where": {
                        "id": "$apple"
                    }
                }
                """;

        assertThrows(NotFoundException.class, () -> build(productMetadata, json));
    }

    @Test
    void query_with_select_array() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$select": ["id", "name"]
                }
                """;

        SpeedyQuery query = build(productMetadata, json);

        assertNotNull(query);
        assertTrue(query.getSelect().contains("id"));
        assertTrue(query.getSelect().contains("name"));
        assertEquals(2, query.getSelect().size());
    }

    @Test
    void query_with_select_single_field() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$select": "id"
                }
                """;

        SpeedyQuery query = build(productMetadata, json);

        assertNotNull(query);
        assertTrue(query.getSelect().contains("id"));
    }

    @Test
    void query_with_page_size_exceeding_max_throws() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$page": {
                        "$size": 50
                    }
                }
                """;

        assertThrows(BadRequestException.class, () -> build(productMetadata, json, 10, 20));
    }

    @Test
    void query_with_default_page_size_clamped_to_max() throws Exception {
        String json = """
                {
                    "$from": "Product"
                }
                """;

        SpeedyQuery query = build(productMetadata, json, 5, 20);

        assertNotNull(query);
        assertEquals(5, query.getPageInfo().getPageSize());
    }
}
