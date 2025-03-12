package com.github.silent.samurai.speedy.query;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.data.Product;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.MetaModel;

import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonNode2SpeedyValueQueryBuilderTest {

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
    void testBuildWhere_ValidCondition() throws Exception {
        String json = "{\"$from\":\"Product\",\"$where\":{\"id\":\"abcd-efgh\",\"cost\":{\"$eq\":\"0\"}}}";

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
    void testBuildWhere_InvalidWhereClause() throws Exception {
        String json = "{\"$from\":\"Product\",\"$where\":\"invalidWhere\"}";


        when(metaModel.findEntityMetadata("Product"))
                .thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

        rootNode = objectMapper.readTree(json);

        builder = new JsonQueryBuilder(metaModel, "Product", rootNode);

        // Expecting BadRequestException due to invalid where clause
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
    void testBuildOrderBy_Valid() throws Exception {
        String json = "{\"$from\":\"Product\",\"$orderBy\":{\"id\":\"ASC\",\"name\":\"DESC\"}}";


        when(metaModel.findEntityMetadata("Product"))
                .thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

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
    void testBuildOrderBy_InvalidOrderByClause() throws Exception {
        String json = "{\"$from\":\"Product\",\"$orderBy\":{\"id\":\"INVALID_ORDER\"}}";


        when(metaModel.findEntityMetadata("Product"))
                .thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

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
    void testBuildPaging_ValidPaging() throws Exception {
        String json = "{\"$from\":\"Product\",\"$page\":{\"$index\":1,\"$size\":20}}";


        when(metaModel.findEntityMetadata("Product"))
                .thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

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
    void testBuildPaging_MissingPageIndex() throws Exception {
        String json = "{\"$from\":\"Product\",\"$page\":{\"$size\":20}}";


        when(metaModel.findEntityMetadata("Product"))
                .thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

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
    void testBuildExpand_ValidExpand() throws Exception {
        String json = "{\"$from\":\"Product\",\"$expand\":[\"relation\"]}";


        when(metaModel.findEntityMetadata("Product"))
                .thenReturn(StaticEntityMetadata.createEntityMetadata(Product.class));

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
    void testGetFrom_InvalidFromClause() throws Exception {
        String json = "{\"$from\":12345}";

        rootNode = objectMapper.readTree(json);

        assertThrows(BadRequestException.class, () -> new JsonQueryBuilder(metaModel, rootNode));
    }
}
