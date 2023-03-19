package com.github.silent.samurai.request.get;

import com.github.silent.samurai.controllers.SpeedyApiController;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.serializers.ApiAutomateJsonSerializer;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GETRequestParserTest {

    @InjectMocks
    GETRequestParser getRequestParser;

    @Mock
    MetaModelProcessor metaModelProcessor;

    @Mock
    EntityMetadata entityMetadata;

    HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);

    String UriRoot = SpeedyApiController.URI;

    @BeforeEach
    void setUp() {
        Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(entityMetadata);
        Mockito.when(entityMetadata.getKeyFields()).thenReturn(Sets.newHashSet("id"));
    }

    @Test
    void processRequest() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer");
        GETRequestContext GETRequestContext = getRequestParser.process(httpServletRequest);

        assertEquals("Customer", GETRequestContext.getRequest().getResource());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, GETRequestContext.getSerializationType());
    }

    @Test
    void processRequest_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "Customer");
        try {
            GETRequestContext GETRequestContext = getRequestParser.process(httpServletRequest);
        } catch (RuntimeException e) {
            assertEquals("Not a valid URL", e.getMessage());
        }
    }

    @Test
    void processRequest3() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer('1')");
        GETRequestContext GETRequestContext = getRequestParser.process(httpServletRequest);

        assertEquals("Customer", GETRequestContext.getRequest().getResource());
        assertTrue(GETRequestContext.isPrimaryKey());
        assertEquals("1", GETRequestContext.getKeywords().get("id"));
        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, GETRequestContext.getSerializationType());
    }

    @Test
    void processRequest2() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer/");
        GETRequestContext GETRequestContext = getRequestParser.process(httpServletRequest);

        assertEquals("Customer", GETRequestContext.getRequest().getResource());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, GETRequestContext.getSerializationType());
    }

    @Test
    void processRequest4() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer('1')/");
        GETRequestContext GETRequestContext = getRequestParser.process(httpServletRequest);

        assertEquals("Customer", GETRequestContext.getRequest().getResource());
        assertTrue(GETRequestContext.isPrimaryKey());
        assertEquals("1", GETRequestContext.getKeywords().get("id"));
        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, GETRequestContext.getSerializationType());
    }

    @Test
    void processRequest6() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(id='1')");
        GETRequestContext GETRequestContext = getRequestParser.process(httpServletRequest);

        assertEquals("Customer", GETRequestContext.getRequest().getResource());
        assertEquals("1", GETRequestContext.getKeywords().get("id"));
        assertTrue(GETRequestContext.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, GETRequestContext.getSerializationType());
    }

    @Test
    void processRequest6_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(id='1', name='apple')");
        GETRequestContext GETRequestContext = getRequestParser.process(httpServletRequest);

        assertEquals("Customer", GETRequestContext.getRequest().getResource());
        assertEquals("apple", GETRequestContext.getKeywords().get("name"));
        assertEquals("1", GETRequestContext.getKeywords().get("id"));
        assertFalse(GETRequestContext.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, GETRequestContext.getSerializationType());
    }

    @Test
    void processRequest7() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name='apple')");
        GETRequestContext GETRequestContext = getRequestParser.process(httpServletRequest);

        assertEquals("Customer", GETRequestContext.getRequest().getResource());
        assertEquals("apple", GETRequestContext.getKeywords().get("name"));
        assertFalse(GETRequestContext.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, GETRequestContext.getSerializationType());
    }

    @Test
    void processRequest7_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name='apple?&*')");
        GETRequestContext GETRequestContext = getRequestParser.process(httpServletRequest);

        assertEquals("Customer", GETRequestContext.getRequest().getResource());
        assertEquals("apple?&*", GETRequestContext.getKeywords().get("name"));
        assertFalse(GETRequestContext.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, GETRequestContext.getSerializationType());
    }

    @Test
    void processRequest8() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name='Test-01%42')");
        GETRequestContext GETRequestContext = getRequestParser.process(httpServletRequest);

        assertEquals("Customer", GETRequestContext.getRequest().getResource());
        assertEquals("Test-01B", GETRequestContext.getKeywords().get("name"));
        assertFalse(GETRequestContext.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, GETRequestContext.getSerializationType());
    }

    @Test
    void processRequest9() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer?$format='JSON'");
        GETRequestContext GETRequestContext = getRequestParser.process(httpServletRequest);

        assertEquals("Customer", GETRequestContext.getRequest().getResource());
        assertEquals("JSON", GETRequestContext.getQueryParams().getFirst("$format"));
        assertFalse(GETRequestContext.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, GETRequestContext.getSerializationType());
    }

    @Test
    void processRequest10() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer?$format='JSON'&$metadata='true'");
        GETRequestContext GETRequestContext = getRequestParser.process(httpServletRequest);

        assertEquals("Customer", GETRequestContext.getRequest().getResource());
        assertEquals("JSON", GETRequestContext.getQueryParams().getFirst("$format"));
        assertEquals("true", GETRequestContext.getQueryParams().getFirst("$metadata"));
        assertFalse(GETRequestContext.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, GETRequestContext.getSerializationType());
    }

    @Test
    void processRequest10_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer?$format='JSON&'&$metadata='true'");
        GETRequestContext GETRequestContext = getRequestParser.process(httpServletRequest);

        assertEquals("Customer", GETRequestContext.getRequest().getResource());
        assertEquals("JSON&", GETRequestContext.getQueryParams().getFirst("$format"));
        assertEquals("true", GETRequestContext.getQueryParams().getFirst("$metadata"));
        assertFalse(GETRequestContext.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, GETRequestContext.getSerializationType());
    }

}