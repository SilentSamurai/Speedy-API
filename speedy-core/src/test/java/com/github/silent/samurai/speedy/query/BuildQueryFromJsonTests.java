package com.github.silent.samurai.speedy.query;


import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.data.Product;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Identifier;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;

@ExtendWith(MockitoExtension.class)
class BuildQueryFromJsonTests {

    private MetaModel metaModel;
    private ObjectMapper objectMapper;
    private JsonNode rootNode;
    private JsonQueryBuilder builder;


    @BeforeEach
    void setUp() throws Exception {
        metaModel = mock(MetaModel.class);
        objectMapper = new ObjectMapper();
    }

    /*
    {
        "$from": "Product",
        "$where": {
            "id": "abcd-efgh",
            "cost": {
                "$eq": "0"
            }
        }
    }
     */
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

        // Parse the input JSON
        rootNode = objectMapper.readTree(json);


        when(metaModel.findEntityMetadata("Product"))
                .thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

        // Initialize the builder
        builder = new JsonQueryBuilder(metaModel, "Product", rootNode);

        // Build the query
        SpeedyQuery query = builder.build();

        // Assert that the query is not null
        assertNotNull(query);

        // Get the 'where' condition
        BooleanCondition whereCondition = (BooleanCondition) query.getWhere();

        // Check that the 'where' condition has sub-conditions
        assertNotNull(whereCondition.getConditions());
        assertEquals(2, whereCondition.getConditions().size());  // We have two conditions: 'id' and 'cost'

        // Validate the first condition (id)
        BinaryCondition idCondition = (BinaryCondition) whereCondition.getConditions().get(0);
        assertEquals(ConditionOperator.EQ, idCondition.getOperator());
        assertEquals("id", idCondition.getField().getFieldMetadata().getOutputPropertyName());

        // Validate the second condition (cost)
        BinaryCondition costCondition = (BinaryCondition) whereCondition.getConditions().get(1);
        assertEquals(ConditionOperator.EQ, costCondition.getOperator());
        assertEquals("cost", costCondition.getField().getFieldMetadata().getOutputPropertyName());
    }

    /*
    {
        "$from": "Product",
        "$where": "invalidWhere"
    }
     */
    @Test
    void query_with_invalid_where_type() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$where": "invalidWhere"
                }
                """;


        when(metaModel.findEntityMetadata("Product")).thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

        rootNode = objectMapper.readTree(json);

        builder = new JsonQueryBuilder(metaModel, "Product", rootNode);

        // Expecting BadRequestException due to an invalid where clause
        assertThrows(BadRequestException.class, builder::build);
    }

    /*
    {
        "$from": "Product",
        "$orderBy": {
            "id": "ASC",
            "name": "DESC"
        }
    }
    */
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


        when(metaModel.findEntityMetadata("Product")).thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

        rootNode = objectMapper.readTree(json);

        builder = new JsonQueryBuilder(metaModel, "Product", rootNode);

        SpeedyQuery query = builder.build();

        assertNotNull(query);
        // Check if sorting was correctly applied

        Set<String> collect = query.getOrderByList().stream().map(by -> by.getFieldMetadata().getOutputPropertyName()).collect(Collectors.toSet());

        assertTrue(collect.contains("id"));
        assertTrue(collect.contains("name"));
    }

    /*
    {
        "$from": "Product",
        "$orderBy": {
            "id": "INVALID_ORDER"
        }
    }
    */
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


        when(metaModel.findEntityMetadata("Product")).thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

        rootNode = objectMapper.readTree(json);

        builder = new JsonQueryBuilder(metaModel, "Product", rootNode);

        // Expecting BadRequestException due to invalid order clause
        assertThrows(BadRequestException.class, builder::build);
    }

    /*
    {
        "$from": "Product",
        "$page": {
            "$index": 1,
            "$size": 20
        }
    }
     */
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


        when(metaModel.findEntityMetadata("Product")).thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

        rootNode = objectMapper.readTree(json);

        builder = new JsonQueryBuilder(metaModel, "Product", rootNode);

        SpeedyQuery query = builder.build();

        assertNotNull(query);
        assertEquals(1, query.getPageInfo().getPageNo());
        assertEquals(20, query.getPageInfo().getPageSize());
    }

    /*
    {
        "$from": "Product",
        "$page": {
            "$size": 20
        }
    }
    */
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


        when(metaModel.findEntityMetadata("Product")).thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

        rootNode = objectMapper.readTree(json);

        builder = new JsonQueryBuilder(metaModel, "Product", rootNode);

        SpeedyQuery query = builder.build();

        assertNotNull(query);
        assertEquals(0, query.getPageInfo().getPageNo());  // Default value
        assertEquals(20, query.getPageInfo().getPageSize());
    }

    /*
    {
        "$from": "Product",
        "$expand": ["relation"]
    }
    */
    @Test
    void query_with_valid_expand_list() throws Exception {
        String json = """
                {
                    "$from": "Product",
                    "$expand": ["relation"]
                }
                """;


        when(metaModel.findEntityMetadata("Product")).thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

        rootNode = objectMapper.readTree(json);

        builder = new JsonQueryBuilder(metaModel, "Product", rootNode);

        SpeedyQuery query = builder.build();

        assertNotNull(query);
        assertTrue(query.getExpand().contains("relation"));
    }

    /*
    {
        "$from": 12345
    }
    */
    @Test
    void query_with_invalid_from_type() throws Exception {
        String json = """
                {
                    "$from": 12345
                }
                """;

        rootNode = objectMapper.readTree(json);

        assertThrows(BadRequestException.class, () -> new JsonQueryBuilder(metaModel, rootNode));
    }

    /*
    {
        "$from": "Product",
        "$where": "invalidWhere"
    }
     */
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


        when(metaModel.findEntityMetadata("Product")).thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

        rootNode = objectMapper.readTree(json);

        builder = new JsonQueryBuilder(metaModel, "Product", rootNode);
        SpeedyQuery query = builder.build();

        BinaryCondition idCondition = (BinaryCondition) query.getWhere().getConditions().get(0);
        assertEquals(ConditionOperator.EQ, idCondition.getOperator());
        assertEquals("id", idCondition.getField().getFieldMetadata().getOutputPropertyName());

        assertInstanceOf(Identifier.class, idCondition.getExpression());
        Identifier identifier = (Identifier) idCondition.getExpression();
        assertEquals("name", identifier.field().getFieldMetadata().getOutputPropertyName());


        BinaryCondition costCondition = (BinaryCondition) query.getWhere().getConditions().get(1);
        assertEquals(ConditionOperator.LT, costCondition.getOperator());
        assertEquals("cost", costCondition.getField().getFieldMetadata().getOutputPropertyName());

        assertInstanceOf(Identifier.class, costCondition.getExpression());
        Identifier costIdentifier = (Identifier) costCondition.getExpression();
        assertEquals("category", costIdentifier.field().getFieldMetadata().getOutputPropertyName());
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

        when(metaModel.findEntityMetadata("Product")).thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

        rootNode = objectMapper.readTree(json);

        assertThrows(NotFoundException.class, () -> new JsonQueryBuilder(metaModel, rootNode).build());
    }
}
