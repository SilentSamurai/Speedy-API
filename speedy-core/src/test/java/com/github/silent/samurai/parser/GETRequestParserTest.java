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

        assertEquals("Customer", requestInfo.resourceType);
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.serializationType);
    }

    @Test
    void processRequest_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "Customer");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("speedy", requestInfo.resourceType);
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.serializationType);
    }

    @Test
    void processRequest3() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(1)");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.resourceType);
        assertTrue(requestInfo.primaryKey);
        assertEquals("1", requestInfo.filters.get("id"));
        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, requestInfo.serializationType);
    }

    @Test
    void processRequest2() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer/");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.resourceType);
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.serializationType);
    }

    @Test
    void processRequest4() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(1)/");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.resourceType);
        assertTrue(requestInfo.primaryKey);
        assertEquals("1", requestInfo.filters.get("id"));
        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, requestInfo.serializationType);
    }

    @Test
    void processRequest6() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(id=1)");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.resourceType);
        assertEquals("1", requestInfo.filters.get("id"));
        assertTrue(requestInfo.primaryKey);
        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, requestInfo.serializationType);
    }

    @Test
    void processRequest6_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(id='1')");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.resourceType);
        assertEquals("1", requestInfo.filters.get("id"));
        assertTrue(requestInfo.primaryKey);
        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, requestInfo.serializationType);
    }

    @Test
    void processRequest7() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name=apple)");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.resourceType);
        assertEquals("apple", requestInfo.filters.get("name"));
        assertFalse(requestInfo.primaryKey);
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.serializationType);
    }

    @Test
    void processRequest7_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name='apple')");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.resourceType);
        assertEquals("apple", requestInfo.filters.get("name"));
        assertFalse(requestInfo.primaryKey);
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.serializationType);
    }

    @Test
    void processRequest8() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name=Test)");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.resourceType);
        assertEquals("Test", requestInfo.filters.get("name"));
        assertFalse(requestInfo.primaryKey);
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.serializationType);
    }

    @Test
    void processRequest9() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer?$format=JSON");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.resourceType);
        assertEquals("JSON", requestInfo.queryParams.getFirst("$format"));
        assertFalse(requestInfo.primaryKey);
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.serializationType);
    }

    @Test
    void processRequest10() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer?$format=JSON&$metadata=true");
        RequestInfo requestInfo = getRequestParser.parse(httpServletRequest);

        assertEquals("Customer", requestInfo.resourceType);
        assertEquals("JSON", requestInfo.queryParams.getFirst("$format"));
        assertEquals("true", requestInfo.queryParams.getFirst("$metadata"));
        assertFalse(requestInfo.primaryKey);
        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.serializationType);
    }

}