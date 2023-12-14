package com.github.silent.samurai.speedy.parser;

import com.github.silent.samurai.speedy.data.Product;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.query.QueryHelper;
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

import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpeedyUriContextTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyUriContextTest.class);

    @Mock
    MetaModelProcessor metaModelProcessor;

    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);

    String UriRoot = SpeedyConstant.URI;

    @BeforeEach
    void setUp() throws NotFoundException {
        Mockito.when(metaModelProcessor.findEntityMetadata("Product")).thenReturn(entityMetadata);
    }

    @Test
    void processRequest() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);
        assertEquals("Product", speedyQuery.getFrom().getName());
        assertFalse(queryHelper.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest_1() {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "Product");
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
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product('1')");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertTrue(queryHelper.isOnlyIdentifiersPresent());
        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("id");
        String rawValueOfValue = queryHelper.getRawValueOfValue(fieldMetadata, String.class);
        assertEquals("1", rawValueOfValue);
    }

    @Test
    void processRequest2() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product/");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        assertEquals("Product", speedyQuery.getFrom().getName());
        assertFalse(queryHelper.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest4() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product('1')/");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertTrue(queryHelper.isOnlyIdentifiersPresent());
        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("id");
        assertEquals("1", queryHelper.getRawValueOfValue(fieldMetadata, String.class));
    }

    @Test
    void processRequest6() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product(id='1')");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertTrue(queryHelper.isOnlyIdentifiersPresent());
        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("id");
        assertEquals("1", queryHelper.getRawValueOfValue(fieldMetadata, String.class));
    }

    @Test
    void processRequest6_1() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product(id='1', name='apple')");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("id");
        assertEquals("1", queryHelper.getRawValueOfValue(fieldMetadata, String.class));

        fieldMetadata = speedyQuery.getFrom().field("name");
        assertEquals("apple", queryHelper.getRawValueOfValue(fieldMetadata, String.class));
    }

    @Test
    void processRequest6_2() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product(id='fdc0bff1-8cc6-446e-a74e-5295039a92dd')");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertTrue(queryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("id");
        assertEquals("fdc0bff1-8cc6-446e-a74e-5295039a92dd", queryHelper.getRawValueOfValue(fieldMetadata, String.class));
    }

    @Test
    void processRequest7() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product(name='apple')");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("name");
        assertEquals("apple", queryHelper.getRawValueOfValue(fieldMetadata, String.class));
    }

    @Test
    void processRequest7_1() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product(name='apple?&*')");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("name");
        assertEquals("apple?&*", queryHelper.getRawValueOfValue(fieldMetadata, String.class));
    }

    @Test
    void processRequest8() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product(name='Test-01%42')");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("name");
        assertEquals("Test-01B", queryHelper.getRawValueOfValue(fieldMetadata, String.class));
    }

    @Test
    void processRequest8_1() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product(cost < 0)");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("cost");
        Optional<BinaryCondition> condition = queryHelper.getCondition(fieldMetadata);

        assertEquals(ConditionOperator.LT, condition.get().getOperator());
        assertEquals(0, queryHelper.getRawValueOfValue(fieldMetadata, Integer.class));

    }

    @Test
    void processRequest8_2() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product(cost <= 25)");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("cost");
        Optional<BinaryCondition> condition = queryHelper.getCondition(fieldMetadata);

        assertEquals(ConditionOperator.LTE, condition.get().getOperator());
        assertEquals(25, queryHelper.getRawValueOfValue(fieldMetadata, Integer.class));
    }

    @Test
    void processRequest8_3() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product(cost == 25)");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("cost");
        Optional<BinaryCondition> condition = queryHelper.getCondition(fieldMetadata);

        assertEquals(ConditionOperator.EQ, condition.get().getOperator());
        assertEquals(25, queryHelper.getRawValueOfValue(fieldMetadata, Integer.class));
    }

    @Test
    void processRequest8_4() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product(cost = 25)");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("cost");
        Optional<BinaryCondition> condition = queryHelper.getCondition(fieldMetadata);

        assertEquals(ConditionOperator.EQ, condition.get().getOperator());
        assertEquals(25, queryHelper.getRawValueOfValue(fieldMetadata, Integer.class));
    }

    @Test
    void processRequest8_5() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product(cost > 25)");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("cost");
        Optional<BinaryCondition> condition = queryHelper.getCondition(fieldMetadata);

        assertEquals(ConditionOperator.GT, condition.get().getOperator());
        assertEquals(25, queryHelper.getRawValueOfValue(fieldMetadata, Integer.class));
    }

    @Test
    void processRequest8_6() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product(cost >= 25)");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("cost");
        Optional<BinaryCondition> condition = queryHelper.getCondition(fieldMetadata);

        assertEquals(ConditionOperator.GTE, condition.get().getOperator());
        assertEquals(25, queryHelper.getRawValueOfValue(fieldMetadata, Integer.class));
    }


    @Test
    void processRequest9() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product?$format='JSON'");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        assertEquals("JSON", parser.getQuery("$format", String.class).get(0));
    }

    @Test
    void processRequest10() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product?$format='JSON'&$metadata='true'");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        assertEquals("JSON", parser.getQuery("$format", String.class).get(0));
        assertEquals(true, parser.getQuery("$metadata", boolean.class).get(0));
    }

    @Test
    void processRequest10_1() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product?$format='JSON&'&$metadata='true'");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        assertEquals("JSON&", parser.getQuery("$format", String.class).get(0));
        assertEquals("true", parser.getQuery("$metadata", String.class).get(0));
    }

    @Test
    void processRequest10_2() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product?intVal='2'&doubleVal='2.0'");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        assertEquals(2, parser.getQuery("intVal", int.class).get(0));
        assertEquals(2.0, parser.getQuery("doubleVal", double.class).get(0));
    }

    @Test
    void processRequest10_3() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product?intVal");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        assertTrue(parser.hasQuery("intVal"));
    }

    @Test
    void processRequest10_4() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product?intVal=2");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        assertEquals(2, parser.getQuery("intVal", int.class).get(0));
    }

    @Test
    void processRequest10_5() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product?intVal=2&doubleVal='2.0'");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        assertEquals(2, parser.getQuery("intVal", int.class).get(0));
        assertEquals(2.0, parser.getQuery("doubleVal", double.class).get(0));
    }

    @Test
    void processRequest11() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product?orderBy='name,id'");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        assertEquals("name,id", parser.getQuery("orderBy", String.class).get(0));
    }

    @Test
    void processRequest11_1() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product?orderBy='name'&orderBy='id'");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        assertEquals("name", parser.getQuery("orderBy", String.class).get(0));
        assertEquals("id", parser.getQuery("orderBy", String.class).get(1));
    }

    @Test
    void processRequest11_2() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product?orderBy=['name','id']");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        Assertions.assertEquals("Product", speedyQuery.getFrom().getName());
        Assertions.assertFalse(queryHelper.isOnlyIdentifiersPresent());

        assertEquals("name", parser.getQuery("orderBy", String.class).get(0));
        assertEquals("id", parser.getQuery("orderBy", String.class).get(1));
    }

    @Test
    void processRequest12_2() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product");
        SpeedyQuery speedyQuery = parser.parse();
        QueryHelper queryHelper = new QueryHelper(speedyQuery);

        FieldMetadata fieldMetadata = speedyQuery.getFrom().field("name");
        Optional<BinaryCondition> condition = queryHelper.getCondition(fieldMetadata);

        assertTrue(condition.isEmpty());

        parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Product(name == 'koil')");
        speedyQuery = parser.parse();
        queryHelper = new QueryHelper(speedyQuery);

        fieldMetadata = speedyQuery.getFrom().field("name");
        String rawValueOfValue = queryHelper.getRawValueOfValue(fieldMetadata, String.class);
        assertNotNull(rawValueOfValue);
        assertEquals("koil", rawValueOfValue);

        condition = queryHelper.getCondition(fieldMetadata);
        assertFalse(condition.isEmpty());
    }


}