package com.github.silent.samurai.speedy.parser;

import com.github.silent.samurai.speedy.data.EntityTestClass;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.models.Operator;
import com.github.silent.samurai.speedy.models.conditions.Condition;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpeedyUriContextTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyUriContextTest.class);

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
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer");
        parser.parse();
        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest_1() {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "Customer");
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
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer('1')");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertTrue(parser.getPrimaryResource().isOnlyIdentifiersPresent());
        assertEquals("1", parser.getPrimaryResource().getFirstFilterValue("id", String.class));
    }

    @Test
    void processRequest2() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer/");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest4() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer('1')/");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertTrue(parser.getPrimaryResource().isOnlyIdentifiersPresent());
        assertEquals("1", parser.getPrimaryResource().getFirstFilterValue("id", String.class));
    }

    @Test
    void processRequest6() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(id='1')");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals("1", parser.getPrimaryResource().getFirstFilterValue("id", String.class));
        assertTrue(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest6_1() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(id='1', name='apple')");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals("apple", parser.getPrimaryResource().getFirstFilterValue("name", String.class));
        assertEquals("1", parser.getPrimaryResource().getFirstFilterValue("id", String.class));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest6_2() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(id='fdc0bff1-8cc6-446e-a74e-5295039a92dd')");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals("fdc0bff1-8cc6-446e-a74e-5295039a92dd", parser.getPrimaryResource().getFirstFilterValue("id", String.class));
        assertTrue(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest7() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(name='apple')");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals("apple", parser.getPrimaryResource().getFirstFilterValue("name", String.class));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest7_1() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(name='apple?&*')");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals("apple?&*", parser.getPrimaryResource().getFirstFilterValue("name", String.class));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(name='Test-01%42')");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals("Test-01B", parser.getPrimaryResource().getFirstFilterValue("name", String.class));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8_1() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(cost < 0)");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals(Operator.LT, parser.getPrimaryResource().getFirstConditionByField("cost").getOperator());
        assertEquals(0, parser.getPrimaryResource().getFirstFilterValue("cost", Integer.class));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8_2() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(cost <= 25)");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals(Operator.LTE, parser.getPrimaryResource().getFirstConditionByField("cost").getOperator());
        assertEquals(25, parser.getPrimaryResource().getFirstFilterValue("cost", Integer.class));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8_3() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(cost == 25)");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals(Operator.EQ, parser.getPrimaryResource().getFirstConditionByField("cost").getOperator());
        assertEquals(25, parser.getPrimaryResource().getFirstFilterValue("cost", Integer.class));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8_4() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(cost = 25)");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals(Operator.EQ, parser.getPrimaryResource().getFirstConditionByField("cost").getOperator());
        assertEquals(25, parser.getPrimaryResource().getFirstFilterValue("cost", Integer.class));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8_5() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(cost > 25)");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals(Operator.GT, parser.getPrimaryResource().getFirstConditionByField("cost").getOperator());
        assertEquals(25, parser.getPrimaryResource().getFirstFilterValue("cost", Integer.class));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest8_6() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(cost >= 25)");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals(Operator.GTE, parser.getPrimaryResource().getFirstConditionByField("cost").getOperator());
        assertEquals(25, parser.getPrimaryResource().getFirstFilterValue("cost", Integer.class));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }


    @Test
    void processRequest9() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer?$format='JSON'");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals("JSON", parser.getQuery("$format", String.class).get(0));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest10() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer?$format='JSON'&$metadata='true'");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals("JSON", parser.getQuery("$format", String.class).get(0));
        assertEquals(true, parser.getQuery("$metadata", boolean.class).get(0));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest10_1() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer?$format='JSON&'&$metadata='true'");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals("JSON&", parser.getQuery("$format", String.class).get(0));
        assertEquals("true", parser.getQuery("$metadata", String.class).get(0));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest10_2() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer?intVal='2'&doubleVal='2.0'");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals(2, parser.getQuery("intVal", int.class).get(0));
        assertEquals(2.0, parser.getQuery("doubleVal", double.class).get(0));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest10_3() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer?intVal");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertTrue(parser.hasQuery("intVal"));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest10_4() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer?intVal=2");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals(2, parser.getQuery("intVal", int.class).get(0));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest10_5() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer?intVal=2&doubleVal='2.0'");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals(2, parser.getQuery("intVal", int.class).get(0));
        assertEquals(2.0, parser.getQuery("doubleVal", double.class).get(0));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest11() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer?orderBy='name,id'");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals("name,id", parser.getQuery("orderBy", String.class).get(0));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest11_1() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer?orderBy='name'&orderBy='id'");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals("name", parser.getQuery("orderBy", String.class).get(0));
        assertEquals("id", parser.getQuery("orderBy", String.class).get(1));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest11_2() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer?orderBy=['name','id']");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals("name", parser.getQuery("orderBy", String.class).get(0));
        assertEquals("id", parser.getQuery("orderBy", String.class).get(1));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

    @Test
    void processRequest12_2() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer");
        parser.parse();
        try {
            List<String> names = parser.getPrimaryResource().getFilterValuesByField("name", String.class);
            assertNotNull(names);
            assertTrue(names.isEmpty());
        } catch (NotFoundException e) {
            assertEquals("keyword not found", e.getMessage());
        }

        parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(name == 'koil')");
        parser.parse();
        List<String> names = parser.getPrimaryResource().getFilterValuesByField("name", String.class);
        assertNotNull(names);
        assertFalse(names.isEmpty());
        assertEquals("koil", names.get(0));

        List<? extends Condition> conditions = parser.getPrimaryResource().getConditionsByField("name");
        assertNotNull(conditions);
        assertFalse(conditions.isEmpty());
    }


}