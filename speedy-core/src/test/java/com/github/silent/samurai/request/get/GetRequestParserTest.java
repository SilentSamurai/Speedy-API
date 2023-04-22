package com.github.silent.samurai.request.get;

import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.IResponseSerializer;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.interfaces.SpeedyConstant;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GetRequestParserTest {


    GetRequestParser getRequestParser;

    @Mock
    MetaModelProcessor metaModelProcessor;

    @Mock
    EntityMetadata entityMetadata;

    @Mock
    EntityManager entityManager;


    GetRequestContext context;

    @Mock
    HttpServletRequest httpServletRequest;

    String UriRoot = SpeedyConstant.URI;

    @BeforeEach
    void setUp() throws UnsupportedEncodingException {
        context = new GetRequestContext(httpServletRequest, metaModelProcessor, entityManager);
        getRequestParser = new GetRequestParser(context);
        Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(entityMetadata);
//        Mockito.when(entityMetadata.getKeyFieldNames()).thenReturn(Sets.newHashSet("id"));
    }

    @Test
    void processRequest() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer");
        getRequestParser.process();
        assertEquals("Customer", context.getRequest().getResource());
        assertEquals(IResponseSerializer.MULTIPLE_ENTITY, context.getSerializationType());
    }

    @Test
    void processRequest_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "Customer");
        try {
            getRequestParser.process();
        } catch (RuntimeException e) {
            assertEquals("Not a valid URL", e.getMessage());
        }
    }

    @Test
    void processRequest3() throws UnsupportedEncodingException {
        Mockito.when(entityMetadata.getKeyFieldNames()).thenReturn(Sets.newHashSet("id"));
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer('1')");
        getRequestParser.process();

        assertEquals("Customer", context.getRequest().getResource());
        assertTrue(context.isPrimaryKey());
        assertEquals("1", context.getKeywords().get("id"));
        assertEquals(IResponseSerializer.SINGLE_ENTITY, context.getSerializationType());
    }

    @Test
    void processRequest2() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer/");
        getRequestParser.process();

        assertEquals("Customer", context.getRequest().getResource());
        assertEquals(IResponseSerializer.MULTIPLE_ENTITY, context.getSerializationType());
    }

    @Test
    void processRequest4() throws UnsupportedEncodingException {
        Mockito.when(entityMetadata.getKeyFieldNames()).thenReturn(Sets.newHashSet("id"));
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer('1')/");
        getRequestParser.process();

        assertEquals("Customer", context.getRequest().getResource());
        assertTrue(context.isPrimaryKey());
        assertEquals("1", context.getKeywords().get("id"));
        assertEquals(IResponseSerializer.SINGLE_ENTITY, context.getSerializationType());
    }

    @Test
    void processRequest6() throws UnsupportedEncodingException {
        Mockito.when(entityMetadata.getKeyFieldNames()).thenReturn(Sets.newHashSet("id"));
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(id='1')");
        getRequestParser.process();

        assertEquals("Customer", context.getRequest().getResource());
        assertEquals("1", context.getKeywords().get("id"));
        assertTrue(context.isPrimaryKey());
        assertEquals(IResponseSerializer.SINGLE_ENTITY, context.getSerializationType());
    }

    @Test
    void processRequest6_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(id='1', name='apple')");
        getRequestParser.process();

        assertEquals("Customer", context.getRequest().getResource());
        assertEquals("apple", context.getKeywords().get("name"));
        assertEquals("1", context.getKeywords().get("id"));
        assertFalse(context.isPrimaryKey());
        assertEquals(IResponseSerializer.MULTIPLE_ENTITY, context.getSerializationType());
    }

    @Test
    void processRequest7() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name='apple')");
        getRequestParser.process();

        assertEquals("Customer", context.getRequest().getResource());
        assertEquals("apple", context.getKeywords().get("name"));
        assertFalse(context.isPrimaryKey());
        assertEquals(IResponseSerializer.MULTIPLE_ENTITY, context.getSerializationType());
    }

    @Test
    void processRequest7_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name='apple?&*')");
        getRequestParser.process();

        assertEquals("Customer", context.getRequest().getResource());
        assertEquals("apple?&*", context.getKeywords().get("name"));
        assertFalse(context.isPrimaryKey());
        assertEquals(IResponseSerializer.MULTIPLE_ENTITY, context.getSerializationType());
    }

    @Test
    void processRequest8() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer(name='Test-01%42')");
        getRequestParser.process();

        assertEquals("Customer", context.getRequest().getResource());
        assertEquals("Test-01B", context.getKeywords().get("name"));
        assertFalse(context.isPrimaryKey());
        assertEquals(IResponseSerializer.MULTIPLE_ENTITY, context.getSerializationType());
    }

    @Test
    void processRequest9() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer?$format='JSON'");
        getRequestParser.process();

        assertEquals("Customer", context.getRequest().getResource());
        assertEquals("JSON", context.getQueryParams().getFirst("$format"));
        assertFalse(context.isPrimaryKey());
        assertEquals(IResponseSerializer.MULTIPLE_ENTITY, context.getSerializationType());
    }

    @Test
    void processRequest10() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer?$format='JSON'&$metadata='true'");
        getRequestParser.process();

        assertEquals("Customer", context.getRequest().getResource());
        assertEquals("JSON", context.getQueryParams().getFirst("$format"));
        assertEquals("true", context.getQueryParams().getFirst("$metadata"));
        assertFalse(context.isPrimaryKey());
        assertEquals(IResponseSerializer.MULTIPLE_ENTITY, context.getSerializationType());
    }

    @Test
    void processRequest10_1() throws UnsupportedEncodingException {
        Mockito.when(httpServletRequest.getRequestURI()).thenReturn(UriRoot + "/Customer?$format='JSON&'&$metadata='true'");
        getRequestParser.process();

        assertEquals("Customer", context.getRequest().getResource());
        assertEquals("JSON&", context.getQueryParams().getFirst("$format"));
        assertEquals("true", context.getQueryParams().getFirst("$metadata"));
        assertFalse(context.isPrimaryKey());
        assertEquals(IResponseSerializer.MULTIPLE_ENTITY, context.getSerializationType());
    }

}