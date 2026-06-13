package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParserProvider;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyCreateBody;
import com.github.silent.samurai.speedy.models.SpeedyDeleteBody;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;
import com.github.silent.samurai.speedy.request.RequestContext;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserSelectionHandlerTest {

    private final ConversionContext conversionContext = ConversionContext.withDefaults();

    private ContentNegotiationManager manager(Map<String, IRequestBodyParserProvider> parserProviders) {
        return new ContentNegotiationManager(parserProviders, Collections.emptyMap());
    }

    @Test
    void missingHeader_selectsJsonProvider() throws SpeedyHttpException {
        RequestContext context = new RequestContext();
        context.put(new SpeedyHeaders(Collections.emptyMap()));
        context.put(conversionContext);

        ParserSelectionHandler handler = new ParserSelectionHandler(manager(mapOf(new JsonParserProvider())));
        handler.process(context);

        assertTrue(context.has(IRequestBodyParser.class));
        assertEquals("application/json", context.get(IRequestBodyParser.class).getContentType());
    }

    @Test
    void wildcardHeader_selectsJsonProvider() throws SpeedyHttpException {
        RequestContext context = new RequestContext();
        context.put(headers("Content-Type", "*/*"));
        context.put(conversionContext);

        ParserSelectionHandler handler = new ParserSelectionHandler(
                manager(mapOf(new JsonParserProvider(), new TextParserProvider())));
        handler.process(context);

        assertEquals("application/json", context.get(IRequestBodyParser.class).getContentType());
    }

    @Test
    void unsupportedHeader_defaultsToJsonProvider() throws SpeedyHttpException {
        RequestContext context = new RequestContext();
        context.put(headers("Content-Type", "application/xml"));
        context.put(conversionContext);

        ParserSelectionHandler handler = new ParserSelectionHandler(manager(mapOf(new JsonParserProvider())));
        handler.process(context);

        assertEquals("application/json", context.get(IRequestBodyParser.class).getContentType());
    }

    @Test
    void matchingHeader_selectsRequestedProvider() throws SpeedyHttpException {
        RequestContext context = new RequestContext();
        context.put(headers("Content-Type", "text/plain"));
        context.put(conversionContext);

        ParserSelectionHandler handler = new ParserSelectionHandler(
                manager(mapOf(new JsonParserProvider(), new TextParserProvider())));
        handler.process(context);

        assertEquals("text/plain", context.get(IRequestBodyParser.class).getContentType());
    }

    @Test
    void matchingHeaderWithCharset_selectsRequestedProvider() throws SpeedyHttpException {
        RequestContext context = new RequestContext();
        context.put(headers("Content-Type", "text/plain; charset=utf-8"));
        context.put(conversionContext);

        ParserSelectionHandler handler = new ParserSelectionHandler(
                manager(mapOf(new JsonParserProvider(), new TextParserProvider())));
        handler.process(context);

        assertEquals("text/plain", context.get(IRequestBodyParser.class).getContentType());
    }

    @Test
    void noProviders_throwsInternalServerError() {
        RequestContext context = new RequestContext();
        context.put(headers("Content-Type", "application/json"));
        context.put(conversionContext);

        ParserSelectionHandler handler = new ParserSelectionHandler(manager(Collections.emptyMap()));
        assertThrows(InternalServerError.class, () -> handler.process(context));
    }

    @Test
    void noJsonProvider_throwsInternalServerError() {
        RequestContext context = new RequestContext();
        context.put(headers("Content-Type", "application/json"));
        context.put(conversionContext);

        ParserSelectionHandler handler = new ParserSelectionHandler(manager(mapOf(new TextParserProvider())));
        assertThrows(InternalServerError.class, () -> handler.process(context));
    }

    private static SpeedyHeaders headers(String name, String value) {
        return new SpeedyHeaders(Collections.singletonMap(name, value));
    }

    private static Map<String, IRequestBodyParserProvider> mapOf(IRequestBodyParserProvider... providers) {
        Map<String, IRequestBodyParserProvider> map = new HashMap<>();
        for (IRequestBodyParserProvider provider : providers) {
            map.put(provider.getContentType().toLowerCase(), provider);
        }
        return map;
    }

    static class JsonParserProvider implements IRequestBodyParserProvider {
        @Override
        public String getContentType() {
            return "application/json";
        }

        @Override
        public IRequestBodyParser create(ConversionContext context) {
            return new StubParser("application/json");
        }
    }

    static class TextParserProvider implements IRequestBodyParserProvider {
        @Override
        public String getContentType() {
            return "text/plain";
        }

        @Override
        public IRequestBodyParser create(ConversionContext context) {
            return new StubParser("text/plain");
        }
    }

    static class StubParser implements IRequestBodyParser {

        private final String contentType;

        StubParser(String contentType) {
            this.contentType = contentType;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public SpeedyQuery parseQuery(byte[] rawBody, MetaModel metaModel, SpeedyQuery baseQuery,
                                      int maxPageSize, int defaultPageSize) throws SpeedyHttpException {
            return null;
        }

        @Override
        public SpeedyCreateBody parseCreate(byte[] rawBody, EntityMetadata entity, TransactionMode mode,
                                            QueryProcessor queryProcessor) throws SpeedyHttpException {
            return null;
        }

        @Override
        public SpeedyUpdateBody parseUpdate(byte[] rawBody, EntityMetadata entity,
                                            QueryProcessor queryProcessor) throws SpeedyHttpException {
            return null;
        }

        @Override
        public SpeedyDeleteBody parseDelete(byte[] rawBody, EntityMetadata entity, TransactionMode mode,
                                            QueryProcessor queryProcessor) throws SpeedyHttpException {
            return null;
        }
    }
}
