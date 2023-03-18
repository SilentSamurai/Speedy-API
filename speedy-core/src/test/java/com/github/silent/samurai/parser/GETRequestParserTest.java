package com.github.silent.samurai.parser;

import com.github.silent.samurai.SpeedyFactory;
import com.github.silent.samurai.TestApplication;
import com.github.silent.samurai.controllers.SpeedyApiController;
import com.github.silent.samurai.metamodel.JpaMetaModelProcessor;
import com.github.silent.samurai.metamodel.RequestInfo;
import com.github.silent.samurai.serializers.ApiAutomateJsonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
class GETRequestParserTest {

    @Autowired
    JpaMetaModelProcessor jpaMetaModelProcessor;

    @Autowired
    SpeedyFactory speedyFactory;

    GETRequestParser getRequestParser;

    HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);

    String UriRoot = SpeedyApiController.URI;

    @BeforeEach
    void setUp() {
        getRequestParser = new GETRequestParser(jpaMetaModelProcessor);
    }

    @Test
    void processRequest() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.getRequest().getResource());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.getSerializationType());
    }

    @Test
    void processRequest_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "Customer");
        try {
            RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);
        } catch (RuntimeException e) {
            assertEquals("Not a valid URL", e.getMessage());
        }
    }

    @Test
    void processRequest3() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer('1')");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.getRequest().getResource());
        assertTrue(requestInfo.isPrimaryKey());
        assertEquals("1", requestInfo.getKeywords().get("id"));
        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, requestInfo.getSerializationType());
    }

    @Test
    void processRequest2() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer/");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.getRequest().getResource());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.getSerializationType());
    }

    @Test
    void processRequest4() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer('1')/");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.getRequest().getResource());
        assertTrue(requestInfo.isPrimaryKey());
        assertEquals("1", requestInfo.getKeywords().get("id"));
        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, requestInfo.getSerializationType());
    }

    @Test
    void processRequest6() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(id='1')");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.getRequest().getResource());
        assertEquals("1", requestInfo.getKeywords().get("id"));
        assertTrue(requestInfo.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, requestInfo.getSerializationType());
    }

    @Test
    void processRequest6_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(id='1')");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.getRequest().getResource());
        assertEquals("1", requestInfo.getKeywords().get("id"));
        assertTrue(requestInfo.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, requestInfo.getSerializationType());
    }

    @Test
    void processRequest7() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name='apple')");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.getRequest().getResource());
        assertEquals("apple", requestInfo.getKeywords().get("name"));
        assertFalse(requestInfo.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.getSerializationType());
    }

    @Test
    void processRequest7_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name='apple')");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.getRequest().getResource());
        assertEquals("apple", requestInfo.getKeywords().get("name"));
        assertFalse(requestInfo.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.getSerializationType());
    }

    @Test
    void processRequest8() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name='Test')");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.getRequest().getResource());
        assertEquals("Test", requestInfo.getKeywords().get("name"));
        assertFalse(requestInfo.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.getSerializationType());
    }

    @Test
    void processRequest9() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer?$format='JSON'");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.getRequest().getResource());
        assertEquals("JSON", requestInfo.getQueryParams().getFirst("$format"));
        assertFalse(requestInfo.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.getSerializationType());
    }

    @Test
    void processRequest10() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer?$format='JSON&'$metadata='true'");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.getRequest().getResource());
        assertEquals("JSON", requestInfo.getQueryParams().getFirst("$format"));
        assertEquals("true", requestInfo.getQueryParams().getFirst("$metadata"));
        assertFalse(requestInfo.isPrimaryKey());
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.getSerializationType());
    }

}