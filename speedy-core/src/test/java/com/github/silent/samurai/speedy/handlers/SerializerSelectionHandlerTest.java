package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.engine.ContentNegotiationManager;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.models.SpeedyBatchResponse;
import com.github.silent.samurai.speedy.models.SpeedyCountResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.models.SpeedyErrorResponse;
import com.github.silent.samurai.speedy.models.SpeedyMetadataResponse;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializerSelectionHandlerTest {

    private final ConversionContext conversionContext = ConversionContext.withDefaults();
    private final MetaModel metaModel = Mockito.mock(MetaModel.class);

    private ContentNegotiationManager manager(Map<String, ISpeedyIoProvider> providerMap) {
        return new ContentNegotiationManager(providerMap);
    }

    @Test
    void missingAcceptHeader_selectsJsonProvider() throws SpeedyHttpException {
        SpeedyContext context = createContext(null);

        SerializerSelectionHandler handler = new SerializerSelectionHandler(manager(mapOf(new JsonSerializerProvider())));
        handler.process(context);

        assertTrue(context.has(IResponseSerializerV2.class));
        assertEquals("application/json", context.get(IResponseSerializerV2.class).getContentType());
    }

    @Test
    void wildcardAcceptHeader_selectsJsonProvider() throws SpeedyHttpException {
        SpeedyContext context = createContext("*/*");

        SerializerSelectionHandler handler = new SerializerSelectionHandler(
                manager(mapOf(new JsonSerializerProvider(), new TextSerializerProvider())));
        handler.process(context);

        assertEquals("application/json", context.get(IResponseSerializerV2.class).getContentType());
    }

    @Test
    void unsupportedAcceptHeader_defaultsToJsonProvider() throws SpeedyHttpException {
        SpeedyContext context = createContext("application/xml");

        SerializerSelectionHandler handler = new SerializerSelectionHandler(manager(mapOf(new JsonSerializerProvider())));
        handler.process(context);

        assertEquals("application/json", context.get(IResponseSerializerV2.class).getContentType());
    }

    @Test
    void matchingAcceptHeader_selectsRequestedProvider() throws SpeedyHttpException {
        SpeedyContext context = createContext("text/plain");

        SerializerSelectionHandler handler = new SerializerSelectionHandler(
                manager(mapOf(new JsonSerializerProvider(), new TextSerializerProvider())));
        handler.process(context);

        assertEquals("text/plain", context.get(IResponseSerializerV2.class).getContentType());
    }

    @Test
    void matchingAcceptHeaderWithCharset_selectsRequestedProvider() throws SpeedyHttpException {
        SpeedyContext context = createContext("text/plain; charset=utf-8");

        SerializerSelectionHandler handler = new SerializerSelectionHandler(
                manager(mapOf(new JsonSerializerProvider(), new TextSerializerProvider())));
        handler.process(context);

        assertEquals("text/plain", context.get(IResponseSerializerV2.class).getContentType());
    }

    @Test
    void noProviders_throwsInternalServerError() {
        SpeedyContext context = createContext("application/json");

        SerializerSelectionHandler handler = new SerializerSelectionHandler(manager(Collections.emptyMap()));
        assertThrows(InternalServerError.class, () -> handler.process(context));
    }

    @Test
    void noJsonProvider_throwsInternalServerError() {
        SpeedyContext context = createContext("application/json");

        SerializerSelectionHandler handler = new SerializerSelectionHandler(manager(mapOf(new TextSerializerProvider())));
        assertThrows(InternalServerError.class, () -> handler.process(context));
    }

    private SpeedyContext createContext(String acceptHeader) {
        Map<String, String> headerMap = new HashMap<>();
        if (acceptHeader != null) {
            headerMap.put("Accept", acceptHeader);
        }

        SpeedyContext context = new SpeedyContext();
        context.put(new SpeedyHeaders(headerMap));
        context.put(MetaModel.class, metaModel);
        context.put(conversionContext);
        return context;
    }

    private static Map<String, ISpeedyIoProvider> mapOf(ISpeedyIoProvider... providers) {
        Map<String, ISpeedyIoProvider> map = new HashMap<>();
        for (ISpeedyIoProvider provider : providers) {
            map.put(provider.getContentType().toLowerCase(), provider);
        }
        return map;
    }

    static class JsonSerializerProvider implements ISpeedyIoProvider {
        @Override
        public String getContentType() { return "application/json"; }

        @Override
        public IResponseSerializerV2 createSerializer(MetaModel metaModel, ConversionContext context) { return new StubSerializer("application/json"); }

        @Override
        public IRequestBodyParser createParser(ConversionContext context) { return null; }

        @Override
        public void contributeModule(ConversionContext ctx) {}
    }

    static class TextSerializerProvider implements ISpeedyIoProvider {
        @Override
        public String getContentType() { return "text/plain"; }

        @Override
        public IResponseSerializerV2 createSerializer(MetaModel metaModel, ConversionContext context) { return new StubSerializer("text/plain"); }

        @Override
        public IRequestBodyParser createParser(ConversionContext context) { return null; }

        @Override
        public void contributeModule(ConversionContext ctx) {}
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

        @Override
        public void writeError(SpeedyErrorResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        }

        @Override
        public void writeMetadata(SpeedyMetadataResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        }
    }
}
