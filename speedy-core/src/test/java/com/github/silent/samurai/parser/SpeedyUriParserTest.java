package com.github.silent.samurai.parser;

import com.github.silent.samurai.data.EntityTestClass;
import com.github.silent.samurai.data.StaticEntityMetadata;
import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.interfaces.SpeedyConstant;
import com.github.silent.samurai.models.Operator;
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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpeedyUriParserTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyUriParserTest.class);

    @Mock
    MetaModelProcessor metaModelProcessor;

    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(EntityTestClass.class);

    String UriRoot = SpeedyConstant.URI;

    @BeforeEach
    void setUp() throws NotFoundException {
        Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(entityMetadata);
    }

    @Test
    void processRequest() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer");
        parser.parse();
        assertEquals("Customer", parser.getResource());
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest_1() {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "Customer");
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
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer('1')");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertTrue(parser.isOnlyIdentifiersPresent());
        assertEquals("1", parser.getFirstFilterValue("id", String.class));
    }

    @Test
    void processRequest2() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer/");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest4() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer('1')/");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertTrue(parser.isOnlyIdentifiersPresent());
        assertEquals("1", parser.getFirstFilterValue("id", String.class));
    }

    @Test
    void processRequest6() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer(id='1')");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals("1", parser.getFirstFilterValue("id", String.class));
        assertTrue(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest6_1() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer(id='1', name='apple')");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals("apple", parser.getFirstFilterValue("name", String.class));
        assertEquals("1", parser.getFirstFilterValue("id", String.class));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest6_2() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer(id='fdc0bff1-8cc6-446e-a74e-5295039a92dd')");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals("fdc0bff1-8cc6-446e-a74e-5295039a92dd", parser.getFirstFilterValue("id", String.class));
        assertTrue(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest7() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer(name='apple')");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals("apple", parser.getFirstFilterValue("name", String.class));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest7_1() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer(name='apple?&*')");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals("apple?&*", parser.getFirstFilterValue("name", String.class));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer(name='Test-01%42')");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals("Test-01B", parser.getFirstFilterValue("name", String.class));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8_1() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer(cost < 0)");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals(Operator.LT, parser.getFirstConditionByField("cost").getOperator());
        assertEquals(0, parser.getFirstFilterValue("cost", Integer.class));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8_2() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer(cost <= 25)");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals(Operator.LTE, parser.getFirstConditionByField("cost").getOperator());
        assertEquals(25, parser.getFirstFilterValue("cost", Integer.class));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8_3() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer(cost == 25)");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals(Operator.EQ, parser.getFirstConditionByField("cost").getOperator());
        assertEquals(25, parser.getFirstFilterValue("cost", Integer.class));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8_4() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer(cost = 25)");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals(Operator.EQ, parser.getFirstConditionByField("cost").getOperator());
        assertEquals(25, parser.getFirstFilterValue("cost", Integer.class));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8_5() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer(cost > 25)");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals(Operator.GT, parser.getFirstConditionByField("cost").getOperator());
        assertEquals(25, parser.getFirstFilterValue("cost", Integer.class));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8_6() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer(cost >= 25)");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals(Operator.GTE, parser.getFirstConditionByField("cost").getOperator());
        assertEquals(25, parser.getFirstFilterValue("cost", Integer.class));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }


    @Test
    void processRequest9() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer?$format='JSON'");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals("JSON", parser.getQuery("$format", String.class).get(0));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest10() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer?$format='JSON'&$metadata='true'");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals("JSON", parser.getQuery("$format", String.class).get(0));
        assertEquals(true, parser.getQuery("$metadata", boolean.class).get(0));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest10_1() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer?$format='JSON&'&$metadata='true'");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals("JSON&", parser.getQuery("$format", String.class).get(0));
        assertEquals("true", parser.getQuery("$metadata", String.class).get(0));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest10_2() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer?intVal='2'&doubleVal='2.0'");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals(2, parser.getQuery("intVal", int.class).get(0));
        assertEquals(2.0, parser.getQuery("doubleVal", double.class).get(0));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest10_3() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer?intVal");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertTrue(parser.hasQuery("intVal"));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest10_4() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer?intVal=2");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals(2, parser.getQuery("intVal", int.class).get(0));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest10_5() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer?intVal=2&doubleVal='2.0'");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals(2, parser.getQuery("intVal", int.class).get(0));
        assertEquals(2.0, parser.getQuery("doubleVal", double.class).get(0));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest11() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer?orderBy='name,id'");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals("name,id", parser.getQuery("orderBy", String.class).get(0));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest11_1() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer?orderBy='name'&orderBy='id'");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals("name", parser.getQuery("orderBy", String.class).get(0));
        assertEquals("id", parser.getQuery("orderBy", String.class).get(1));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest11_2() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(metaModelProcessor, UriRoot + "/Customer?orderBy=['name','id']");
        parser.parse();

        assertEquals("Customer", parser.getResource());
        assertEquals("name", parser.getQuery("orderBy", String.class).get(0));
        assertEquals("id", parser.getQuery("orderBy", String.class).get(1));
        assertFalse(parser.isOnlyIdentifiersPresent());
    }


}