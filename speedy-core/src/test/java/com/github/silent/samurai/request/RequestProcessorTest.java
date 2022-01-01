//package com.github.silent.samurai.request;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import com.github.silent.samurai.controllers.ResourceController;
//import com.github.silent.samurai.metamodel.JpaMetaModel;
//import com.github.silent.samurai.metamodel.RequestInfo;
//import com.github.silent.samurai.serializers.ApiAutomateJsonSerializer;
//import org.junit.Ignore;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//
//import javax.servlet.http.HttpServletRequest;
//import java.io.UnsupportedEncodingException;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//
//class RequestProcessorTest {
//
//    JpaMetaModel jpaMetaModel = Mockito.mock(JpaMetaModel.class);
//
//    RequestProcessor requestProcessor = new RequestProcessor(jpaMetaModel);
//
//    HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
//
//    String UriRoot = ResourceController.URI;
//
//    @BeforeEach
//    void setUp() {
//
//    }
//
//    @Test
//    void processRequest() throws UnsupportedEncodingException {
//        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer");
//        RequestInfo requestInfo = requestProcessor.process(httpServletRequest);
//
//        assertEquals("Customer", requestInfo.resourceType);
//        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.serializationType);
//    }
//
//    @Test
//    void processRequest3() throws UnsupportedEncodingException {
//        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(1)");
//        RequestInfo requestInfo = requestProcessor.process(httpServletRequest);
//
//        assertEquals("Customer", requestInfo.resourceType);
//        assertTrue(requestInfo.primaryKey);
//        assertEquals("1", requestInfo.filters.get("id"));
//        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, requestInfo.serializationType);
//    }
//
//    @Test
//    void processRequest2() throws UnsupportedEncodingException {
//        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer/");
//        RequestInfo requestInfo = requestProcessor.process(httpServletRequest);
//
//        assertEquals("Customer", requestInfo.resourceType);
//        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.serializationType);
//    }
//
//    @Test
//    void processRequest4() throws UnsupportedEncodingException {
//        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(1)/");
//        RequestInfo requestInfo = requestProcessor.process(httpServletRequest);
//
//        assertEquals("Customer", requestInfo.resourceType);
//        assertTrue(requestInfo.primaryKey);
//        assertEquals("1", requestInfo.filters.get("id"));
//        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, requestInfo.serializationType);
//    }
//
//    @Test
//    void processRequest6() throws UnsupportedEncodingException {
//        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(#id=1)");
//        RequestInfo requestInfo = requestProcessor.process(httpServletRequest);
//
//        assertEquals("Customer", requestInfo.resourceType);
//        assertEquals("1", requestInfo.filters.get("id"));
//        assertTrue(requestInfo.primaryKey);
//        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, requestInfo.serializationType);
//    }
//
//    @Test
//    void processRequest7() throws UnsupportedEncodingException {
//        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name=apple)");
//        RequestInfo requestInfo = requestProcessor.process(httpServletRequest);
//
//        assertEquals("Customer", requestInfo.resourceType);
//        assertEquals("apple", requestInfo.filters.get("name"));
//        assertFalse(requestInfo.primaryKey);
//        assertEquals(ApiAutomateJsonSerializer.MULTIPLE_ENTITY, requestInfo.serializationType);
//    }
//
//    @Test
//    void processRequest8() throws UnsupportedEncodingException {
//        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name=Test)");
//        RequestInfo requestInfo = requestProcessor.process(httpServletRequest);
//
//        assertEquals("Customer", requestInfo.resourceType);
//        assertEquals("Test", requestInfo.filters.get("name"));
//        assertTrue(requestInfo.primaryKey);
//        assertEquals(ApiAutomateJsonSerializer.SINGLE_ENTITY, requestInfo.serializationType);
//    }
//
//}