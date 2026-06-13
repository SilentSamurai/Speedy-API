package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerProvider;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.models.SpeedyBatchResponse;
import com.github.silent.samurai.speedy.models.SpeedyCountResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class SerializerSelectionHandlerTest {

    private final ConversionContext conversionContext = ConversionContext.withDefaults();
    private final MetaModel metaModel = Mockito.mock(MetaModel.class);
    private final EntityMetadata entityMetadata = Mockito.mock(EntityMetadata.class);

    private ContentNegotiationManager manager(Map<String, IResponseSerializerProvider> serializerProviders) {
        return new ContentNegotiationManager(Collections.emptyMap(), serializerProviders);
    }

    @Test
    void missingAcceptHeader_selectsJsonProvider() throws SpeedyHttpException {
        RequestContext context = createContext(null);

        SerializerSelectionHandler handler = new SerializerSelectionHandler(manager(mapOf(new JsonSerializerProvider())));
        handler.process(context);

        assertTrue(context.has(IResponseSerializerV2.class));
        assertEquals("application/json", context.get(IResponseSerializerV2.class).getContentType());
    }

    @Test
    void wildcardAcceptHeader_selectsJsonProvider() throws SpeedyHttpException {
        RequestContext context = createContext("*/*");

        SerializerSelectionHandler handler = new SerializerSelectionHandler(
                manager(mapOf(new JsonSerializerProvider(), new TextSerializerProvider())));
        handler.process(context);

        assertEquals("application/json", context.get(IResponseSerializerV2.class).getContentType());
    }

    @Test
    void unsupportedAcceptHeader_defaultsToJsonProvider() throws SpeedyHttpException {
        RequestContext context = createContext("application/xml");

        SerializerSelectionHandler handler = new SerializerSelectionHandler(manager(mapOf(new JsonSerializerProvider())));
        handler.process(context);

        assertEquals("application/json", context.get(IResponseSerializerV2.class).getContentType());
    }

    @Test
    void matchingAcceptHeader_selectsRequestedProvider() throws SpeedyHttpException {
        RequestContext context = createContext("text/plain");

        SerializerSelectionHandler handler = new SerializerSelectionHandler(
                manager(mapOf(new JsonSerializerProvider(), new TextSerializerProvider())));
        handler.process(context);

        assertEquals("text/plain", context.get(IResponseSerializerV2.class).getContentType());
    }

    @Test
    void matchingAcceptHeaderWithCharset_selectsRequestedProvider() throws SpeedyHttpException {
        RequestContext context = createContext("text/plain; charset=utf-8");

        SerializerSelectionHandler handler = new SerializerSelectionHandler(
                manager(mapOf(new JsonSerializerProvider(), new TextSerializerProvider())));
        handler.process(context);

        assertEquals("text/plain", context.get(IResponseSerializerV2.class).getContentType());
    }

    @Test
    void noProviders_throwsInternalServerError() {
        RequestContext context = createContext("application/json");

        SerializerSelectionHandler handler = new SerializerSelectionHandler(manager(Collections.emptyMap()));
        assertThrows(InternalServerError.class, () -> handler.process(context));
    }

    @Test
    void noJsonProvider_throwsInternalServerError() {
        RequestContext context = createContext("application/json");

        SerializerSelectionHandler handler = new SerializerSelectionHandler(manager(mapOf(new TextSerializerProvider())));
        assertThrows(InternalServerError.class, () -> handler.process(context));
    }

    private RequestContext createContext(String acceptHeader) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("Accept")).thenReturn(acceptHeader);

        SpeedyUriContext uriContext = Mockito.mock(SpeedyUriContext.class);
        SpeedyQueryImpl speedyQuery = Mockito.mock(SpeedyQueryImpl.class);
        when(uriContext.getParsedQuery()).thenReturn(speedyQuery);
        when(speedyQuery.getFrom()).thenReturn(entityMetadata);

        RequestContext context = new RequestContext();
        context.put(HttpServletRequest.class, request);
        context.put(MetaModel.class, metaModel);
        context.put(SpeedyUriContext.class, uriContext);
        context.put(conversionContext);
        return context;
    }

    private static Map<String, IResponseSerializerProvider> mapOf(IResponseSerializerProvider... providers) {
        Map<String, IResponseSerializerProvider> map = new HashMap<>();
        for (IResponseSerializerProvider provider : providers) {
            map.put(provider.getContentType().toLowerCase(), provider);
        }
        return map;
    }

    static class JsonSerializerProvider implements IResponseSerializerProvider {
        @Override
        public String getContentType() {
            return "application/json";
        }

        @Override
        public IResponseSerializerV2 create(MetaModel metaModel, EntityMetadata entityMetadata, ConversionContext context) {
            return new StubSerializer("application/json");
        }
    }

    static class TextSerializerProvider implements IResponseSerializerProvider {
        @Override
        public String getContentType() {
            return "text/plain";
        }

        @Override
        public IResponseSerializerV2 create(MetaModel metaModel, EntityMetadata entityMetadata, ConversionContext context) {
            return new StubSerializer("text/plain");
        }
    }

    static class StubSerializer implements IResponseSerializerV2 {

        private final String contentType;

        StubSerializer(String contentType) {
            this.contentType = contentType;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public void writeEntityList(SpeedyEntityResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        }

        @Override
        public void writeCount(SpeedyCountResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        }

        @Override
        public void writeBatch(SpeedyBatchResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        }
    }
}
