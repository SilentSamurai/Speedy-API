package com.github.silent.samurai.speedy.parser;

import com.github.silent.samurai.speedy.data.ComposedProduct;
import com.github.silent.samurai.speedy.data.Product;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.data.ValueTest;
import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.OrderByOperator;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.interfaces.query.*;
import com.github.silent.samurai.speedy.conversion.registry.JavaTypeRegistry;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.query.SpeedyQueryHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpeedyUriContextTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyUriContextTest.class);
    private final JavaTypeRegistry javaTypeRegistry = JavaTypeRegistry.defaults();
    @Mock
    MetaModel metaModel;
    EntityMetadata productMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
    EntityMetadata vtentity = StaticEntityMetadata.createEntityMetadata(ValueTest.class);
    EntityMetadata composedEntity = StaticEntityMetadata.createEntityMetadata(ComposedProduct.class);
    String UriRoot = SpeedyConstants.URI;

    @BeforeEach
    void setUp() throws NotFoundException {
        Mockito.when(metaModel.findEntityMetadata("Product")).thenReturn(productMetadata);
        Mockito.when(metaModel.findEntityMetadata("ValueTest")).thenReturn(vtentity);
        Mockito.when(metaModel.findEntityMetadata("ComposedProduct")).thenReturn(composedEntity);
    }

    @Test
    void processRequest() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(SpeedyConstants.URI + "/Product").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);
        assertEquals("Product", speedyQuery.getFrom().getName());
        assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest_1() {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(SpeedyConstants.URI + "Product").build();
        try {
            parser.parse();
        } catch (RuntimeException e) {
            assertEquals("Not a valid URL", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Exception ", e);
        }
    }

    @Test
    void processRequest3() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(SpeedyConstants.URI + "/Product?id=1").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        assertTrue(speedyQueryHelper.isOnlyIdentifiersPresent());
        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("id");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertTrue(literal.value().isText());
        assertEquals("1", literal.value().asText());
    }

    @Test
    void processRequest2() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(SpeedyConstants.URI + "/Product/").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        assertEquals("Product", speedyQuery.getFrom().getName());
        assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest4() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(SpeedyConstants.URI + "/Product/?id='1'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        assertTrue(speedyQueryHelper.isOnlyIdentifiersPresent());
        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("id");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertTrue(literal.value().isText());
        assertEquals("1", literal.value().asText());
    }

    @Test
    void processRequest6() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(SpeedyConstants.URI + "/Product").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        assertTrue(speedyQuery.getWhere().getConditions().isEmpty());
    }

    @Test
    void association_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(SpeedyConstants.URI + "/ComposedProduct?productItem.id='1'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("ComposedProduct", speedyQuery.getFrom().getName());
        assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());
        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("productItem");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertTrue(literal.value().isText());
        assertEquals("1", literal.value().asText());
    }

    @Test
    void processRequest6_1() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?id='1'&name='apple'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata idField = speedyQuery.getFrom().field("id");
        Expression idExpression = speedyQueryHelper.getFilterValue(idField).orElseThrow();
        Literal idLiteral = assertInstanceOf(Literal.class, idExpression);
        assertTrue(idLiteral.value().isText());
        assertEquals("1", idLiteral.value().asText());

        FieldMetadata nameField = speedyQuery.getFrom().field("name");
        Expression nameExpression = speedyQueryHelper.getFilterValue(nameField).orElseThrow();
        Literal nameLiteral = assertInstanceOf(Literal.class, nameExpression);
        assertTrue(nameLiteral.value().isText());
        assertEquals("apple", nameLiteral.value().asText());
    }

    @Test
    void processRequest6_2() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?id='fdc0bff1-8cc6-446e-a74e-5295039a92dd'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        assertTrue(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("id");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertTrue(literal.value().isText());
        assertEquals("fdc0bff1-8cc6-446e-a74e-5295039a92dd", literal.value().asText());
    }

    @Test
    void processRequest7() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?name='apple'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("name");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertTrue(literal.value().isText());
        assertEquals("apple", literal.value().asText());
    }

    @Test
    void string_multiple_value() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?name=apple&name=ball&name=cat").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("name");
        Optional<BinaryCondition> condition = speedyQueryHelper.getCondition(fieldMetadata);
        SpeedyValue speedyValue = ((Literal) condition.get().getExpression()).value();
        assertEquals(speedyValue.getValueType(), ValueType.COLLECTION);
        SpeedyCollection speedyCollection = (SpeedyCollection) speedyValue;
        for (SpeedyValue value : speedyCollection.asCollection()) {
            assertEquals(value.getValueType(), ValueType.TEXT);
        }
    }


    @Test
    void processRequest7_1() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?name='apple?&*'").build();
        assertThrows(BadRequestException.class, () -> parser.parse());
    }

    @Test
    void processRequest8() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?name='Test-01%42'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("name");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertTrue(literal.value().isText());
        assertEquals("Test-01B", literal.value().asText());
    }

    @Test
    void processRequest8_1() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?name=Test&cost=12").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata costField = productMetadata.field("cost");
        Expression costExpression = speedyQueryHelper.getFilterValue(costField).orElseThrow();
        Literal costLiteral = assertInstanceOf(Literal.class, costExpression);
        assertEquals(12L, costLiteral.value().asLong());

        FieldMetadata nameField = productMetadata.field("name");
        Expression nameExpression = speedyQueryHelper.getFilterValue(nameField).orElseThrow();
        Literal nameLiteral = assertInstanceOf(Literal.class, nameExpression);
        assertTrue(nameLiteral.value().isText());
        assertEquals("Test", nameLiteral.value().asText());

    }

    @Test
    void processRequest8_3() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?cost=25").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("cost");
        Optional<BinaryCondition> condition = speedyQueryHelper.getCondition(fieldMetadata);

        assertEquals(ConditionOperator.EQ, condition.get().getOperator());
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertEquals(25L, literal.value().asLong());
    }

    @Test
    void processRequest8_4() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?cost = 25").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("cost");
        Optional<BinaryCondition> condition = speedyQueryHelper.getCondition(fieldMetadata);

        assertTrue(condition.isPresent());
        assertEquals(ConditionOperator.EQ, condition.get().getOperator());
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertEquals(25L, literal.value().asLong());
    }

//    @Test
//    void processRequest8_5() throws Exception {
//        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product(cost > 25).build()");
//        SpeedyQuery speedyQuery = parser.parse();
//        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);
//
//        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
//        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());
//
//        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("cost");
//        Optional<BinaryCondition> condition = speedyQueryHelper.getCondition(fieldMetadata);
//
//        assertEquals(ConditionOperator.GT, condition.get().getOperator());
//        assertEquals(25, speedyQueryHelper.getRawValueOfValue(fieldMetadata, Long.class));
//    }

//    @Test
//    void processRequest8_6() throws Exception {
//        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product(cost >= 25).build()");
//        SpeedyQuery speedyQuery = parser.parse();
//        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);
//
//        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
//        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());
//
//        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("cost");
//        Optional<BinaryCondition> condition = speedyQueryHelper.getCondition(fieldMetadata);
//
//        assertEquals(ConditionOperator.GTE, condition.get().getOperator());
//        assertEquals(25, speedyQueryHelper.getRawValueOfValue(fieldMetadata, Long.class));
//    }


    @Test
    void processRequest9() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?$format='JSON'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        assertEquals("JSON", speedyQuery.getResponseFormat());
    }

//    @Test
//    void processRequest10() throws Exception {
//        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?$format='JSON'&$metadata='true'").build();
//        SpeedyQuery speedyQuery = parser.parse();
//        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);
//
//        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
//        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());
//
//        assertEquals("JSON", parser.getQuery("$format", String.class).get(0));
//        assertEquals(true, parser.getQuery("$metadata", boolean.class).get(0));
//    }

    @Test
    void processRequest10_1() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?$format='JSON'&").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        assertEquals("JSON", speedyQuery.getResponseFormat());
    }

    @Test
    void processRequest10_2() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?intVal='2'&doubleVal='2.0'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("ValueTest", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata intValField = vtentity.field("intVal");
        Expression intExpression = speedyQueryHelper.getFilterValue(intValField).orElseThrow();
        Literal intLiteral = assertInstanceOf(Literal.class, intExpression);
        assertEquals(2L, intLiteral.value().asLong());

        FieldMetadata doubleValField = vtentity.field("doubleVal");
        Expression doubleExpression = speedyQueryHelper.getFilterValue(doubleValField).orElseThrow();
        Literal doubleLiteral = assertInstanceOf(Literal.class, doubleExpression);
        assertEquals(2.0, doubleLiteral.value().asDouble(), 0.001);
    }

    @Test
    void processRequest10_3() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?intVal").build();
        assertThrows(BadRequestException.class, () -> parser.parse());
    }

    @Test
    void processRequest10_4() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?intVal=2").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("ValueTest", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = vtentity.field("intVal");
        assertTrue(speedyQueryHelper.isFilterPresent(fieldMetadata));
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertEquals(2L, literal.value().asLong());
    }

    @Test
    void processRequest10_5() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?intVal=2&doubleVal='2.0'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("ValueTest", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata intValField = vtentity.field("intVal");
        Expression intExpression = speedyQueryHelper.getFilterValue(intValField).orElseThrow();
        Literal intLiteral = assertInstanceOf(Literal.class, intExpression);
        assertEquals(2L, intLiteral.value().asLong());

        FieldMetadata doubleValField = vtentity.field("doubleVal");
        Expression doubleExpression = speedyQueryHelper.getFilterValue(doubleValField).orElseThrow();
        Literal doubleLiteral = assertInstanceOf(Literal.class, doubleExpression);
        assertEquals(2.0, doubleLiteral.value().asDouble(), 0.001);
    }

    @Test
    void processRequest11() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?$orderBy='name,id'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata nameField = productMetadata.field("name");
        FieldMetadata idField = productMetadata.field("id");

        Optional<OrderBy> nameOrder = speedyQuery.getOrderByList()
                .stream()
                .filter(orderBy -> orderBy.getFieldMetadata().equals(nameField) && orderBy.getOperator() == OrderByOperator.ASC)
                .findAny();

        Optional<OrderBy> idOrder = speedyQuery.getOrderByList()
                .stream()
                .filter(orderBy -> orderBy.getFieldMetadata().equals(idField) && orderBy.getOperator() == OrderByOperator.ASC)
                .findAny();

        assertTrue(nameOrder.isPresent());
        assertTrue(idOrder.isPresent());
    }

    @Test
    void processRequest11_1() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?$orderBy='name'&$orderBy='id'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata nameField = productMetadata.field("name");
        FieldMetadata idField = productMetadata.field("id");

        Optional<OrderBy> nameOrder = speedyQuery.getOrderByList()
                .stream()
                .filter(orderBy -> orderBy.getFieldMetadata().equals(nameField) && orderBy.getOperator() == OrderByOperator.ASC)
                .findAny();

        Optional<OrderBy> idOrder = speedyQuery.getOrderByList()
                .stream()
                .filter(orderBy -> orderBy.getFieldMetadata().equals(idField) && orderBy.getOperator() == OrderByOperator.ASC)
                .findAny();

        assertTrue(nameOrder.isPresent());
        assertTrue(idOrder.isPresent());
    }

    @Test
    void order_by_desc() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/Product?$orderByDesc='name'&$orderByDesc='id'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());

        FieldMetadata nameField = productMetadata.field("name");
        FieldMetadata idField = productMetadata.field("id");

        Optional<OrderBy> nameOrder = speedyQuery.getOrderByList()
                .stream()
                .filter(orderBy -> orderBy.getFieldMetadata().equals(nameField) && orderBy.getOperator() == OrderByOperator.DESC)
                .findAny();

        Optional<OrderBy> idOrder = speedyQuery.getOrderByList()
                .stream()
                .filter(orderBy -> orderBy.getFieldMetadata().equals(idField) && orderBy.getOperator() == OrderByOperator.DESC)
                .findAny();

        assertTrue(nameOrder.isPresent());
        assertTrue(idOrder.isPresent());
    }


    @Test
    void localDate_value_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?localDate='2024-03-13'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        FieldMetadata fieldMetadata = vtentity.field("localDate");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertTrue(literal.value().isDate());
        assertEquals(LocalDate.of(2024, 3, 13), literal.value().asDate());
    }

    @Test
    void localDate_empty_value_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?localDate=''").build();
        assertThrows(BadRequestException.class, () -> parser.parse());
//        SpeedyQuery speedyQuery = parser.parse();
    }

    @Test
    void localTime_value_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?localTime='12:30:45'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        FieldMetadata fieldMetadata = vtentity.field("localTime");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertTrue(literal.value().isTime());
        assertEquals(LocalTime.of(12, 30, 45), literal.value().asTime());
    }

    @Test
    void localTime_empty_value_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?localTime=''").build();
        assertThrows(BadRequestException.class, () -> parser.parse());
    }

    @Test
    void localDateTime_value_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?localDateTime='2024-03-13T12:30:45'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        FieldMetadata fieldMetadata = vtentity.field("localDateTime");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertTrue(literal.value().isDateTime());
        assertEquals(LocalDateTime.of(2024, 3, 13, 12, 30, 45), literal.value().asDateTime());
    }

    @Test
    void localDateTime_empty_value_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?localDateTime=''").build();
        assertThrows(BadRequestException.class, parser::parse);
    }

    @Test
    void zonedDateTime_value_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?zonedDateTime='2024-03-13T12:30:45%2B05%3A30'").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2024, 3, 13, 12, 30, 45, 0,
                ZoneId.of("Asia/Kolkata"));

        FieldMetadata fieldMetadata = vtentity.field("zonedDateTime");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertTrue(literal.value().isZonedDateTime());
        ZonedDateTime actual = literal.value().asZonedDateTime();

        assertEquals(zonedDateTime.toOffsetDateTime().toString(), actual.toString());
    }

    @Test
    void zonedDateTime_empty_value_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?zonedDateTime=''").build();
        assertThrows(BadRequestException.class, parser::parse);
    }

    @Test
    void boolean_value_test() throws Exception {
        // Given: URL with boolean value
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?booleanVal=true").build();

        // When: Parsing the URL
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        // Then: Validate the parsed boolean value
        FieldMetadata fieldMetadata = vtentity.field("booleanVal");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertEquals(true, literal.value().asBoolean());
    }

    @Test
    void boolean_value_false_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?booleanVal=false").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        FieldMetadata fieldMetadata = vtentity.field("booleanVal");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertEquals(false, literal.value().asBoolean());
    }

    @Test
    void boolean_value_numeric_true_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?booleanVal=1").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        FieldMetadata fieldMetadata = vtentity.field("booleanVal");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertEquals(false, literal.value().asBoolean());
    }

    @Test
    void boolean_value_numeric_false_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?booleanVal=0").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        FieldMetadata fieldMetadata = vtentity.field("booleanVal");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertEquals(false, literal.value().asBoolean());
    }

    @Test
    void boolean_value_invalid_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?booleanVal=invalid").build();
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        FieldMetadata fieldMetadata = vtentity.field("booleanVal");
        Expression expression = speedyQueryHelper.getFilterValue(fieldMetadata).orElseThrow();
        Literal literal = assertInstanceOf(Literal.class, expression);
        assertEquals(false, literal.value().asBoolean());
    }


    @Test
    void page_size_test() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?$pageSize=10&$pageNo=0").build();
        SpeedyQuery speedyQuery = parser.parse();
        assertEquals(0, speedyQuery.getPageInfo().getPageNo());
        assertEquals(10, speedyQuery.getPageInfo().getPageSize());

    }

    @Test
    void page_size_exceeds_max_throws_400() {
        SpeedyUriContext parser = SpeedyUriContext.builder()
                .javaTypeRegistry(javaTypeRegistry)
                .metaModel(metaModel)
                .requestURI(UriRoot + "/ValueTest?$pageSize=100&$pageNo=0")
                .maxPageSize(10)
                .defaultPageSize(20)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, parser::parse);
        assertTrue(ex.getMessage().contains("exceeds maximum allowed page size"));
    }

    @Test
    void default_page_size_is_clamped_to_max() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder()
                .javaTypeRegistry(javaTypeRegistry)
                .metaModel(metaModel)
                .requestURI(UriRoot + "/ValueTest")
                .maxPageSize(5)
                .defaultPageSize(20)
                .build();

        SpeedyQuery speedyQuery = parser.parse();
        assertEquals(5, speedyQuery.getPageInfo().getPageSize());
    }

    @Test
    void select_url_param() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder()
                .javaTypeRegistry(javaTypeRegistry)
                .metaModel(metaModel)
                .requestURI(UriRoot + "/ValueTest?$select=id,name")
                .maxPageSize(100)
                .defaultPageSize(20)
                .build();

        SpeedyQuery speedyQuery = parser.parse();
        assertTrue(speedyQuery.getSelect().contains("id"));
        assertTrue(speedyQuery.getSelect().contains("name"));
        assertEquals(2, speedyQuery.getSelect().size());
    }

    @Test
    void select_url_param_single_field() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder()
                .javaTypeRegistry(javaTypeRegistry)
                .metaModel(metaModel)
                .requestURI(UriRoot + "/ValueTest?$select=id")
                .maxPageSize(100)
                .defaultPageSize(20)
                .build();

        SpeedyQuery speedyQuery = parser.parse();
        assertTrue(speedyQuery.getSelect().contains("id"));
        assertEquals(1, speedyQuery.getSelect().size());
    }

    @Test
    void select_url_param_count() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder()
                .javaTypeRegistry(javaTypeRegistry)
                .metaModel(metaModel)
                .requestURI(UriRoot + "/ValueTest?$select=$count")
                .maxPageSize(100)
                .defaultPageSize(20)
                .build();

        SpeedyQuery speedyQuery = parser.parse();
        assertTrue(speedyQuery.isCountRequest());
        assertFalse(speedyQuery.getSelect().contains("$count"));
    }

    @Test
    void select_url_param_mixed_count_and_fields_should_throw() {
        SpeedyUriContext parser = SpeedyUriContext.builder()
                .javaTypeRegistry(javaTypeRegistry)
                .metaModel(metaModel)
                .requestURI(UriRoot + "/ValueTest?$select=$count,id,name")
                .maxPageSize(100)
                .defaultPageSize(20)
                .build();

        assertThrows(BadRequestException.class, parser::parse);
    }

    @Test
    void default_page_size_unset_uses_20() throws Exception {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest").build();
        SpeedyQuery speedyQuery = parser.parse();
        assertEquals(20, speedyQuery.getPageInfo().getPageSize());
    }

    @Test
    void invalid_page_size_throws() {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?$pageSize=abc").build();
        BadRequestException ex = assertThrows(BadRequestException.class, parser::parse);
        assertTrue(ex.getMessage().contains("Invalid value for $pageSize"));
    }

    @Test
    void invalid_page_no_throws() {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?$pageNo=xyz").build();
        BadRequestException ex = assertThrows(BadRequestException.class, parser::parse);
        assertTrue(ex.getMessage().contains("Invalid value for $pageNo"));
    }

    @Test
    void unknown_query_field_throws() {
        SpeedyUriContext parser = SpeedyUriContext.builder().javaTypeRegistry(javaTypeRegistry).metaModel(metaModel).requestURI(UriRoot + "/ValueTest?typoField=5").build();
        BadRequestException ex = assertThrows(BadRequestException.class, parser::parse);
        assertTrue(ex.getMessage().contains("Unknown query field: 'typoField'"));
    }

}