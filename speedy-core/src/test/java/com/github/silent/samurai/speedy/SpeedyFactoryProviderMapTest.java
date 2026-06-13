package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParserProvider;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeedyFactoryProviderMapTest {

    @Test
    void buildProviderMap_createsCaseInsensitiveLookup() throws SpeedyHttpException {
        JsonProvider provider = new JsonProvider();
        Map<String, IRequestBodyParserProvider> map = SpeedyFactory.buildProviderMap(
                List.of(provider), IRequestBodyParserProvider::getContentType, "IRequestBodyParserProvider");

        assertEquals(1, map.size());
        assertSame(provider, map.get("application/json"));
    }

    @Test
    void buildProviderMap_throwsOnDuplicateContentType() {
        InternalServerError error = assertThrows(InternalServerError.class, () ->
                SpeedyFactory.buildProviderMap(
                        List.of(new JsonProvider(), new JsonProvider()),
                        IRequestBodyParserProvider::getContentType,
                        "IRequestBodyParserProvider"));

        assertTrue(error.getMessage().contains("Duplicate"));
        assertTrue(error.getMessage().contains("application/json"));
    }

    @Test
    void buildProviderMap_throwsOnCaseInsensitiveDuplicateContentType() {
        InternalServerError error = assertThrows(InternalServerError.class, () ->
                SpeedyFactory.buildProviderMap(
                        List.of(new JsonProvider(), new UpperCaseJsonProvider()),
                        IRequestBodyParserProvider::getContentType,
                        "IRequestBodyParserProvider"));

        assertTrue(error.getMessage().contains("Duplicate"));
    }

    static class JsonProvider implements IRequestBodyParserProvider {
        @Override
        public String getContentType() {
            return "application/json";
        }

        @Override
        public IRequestBodyParser create(ConversionContext context) {
            return null;
        }
    }

    static class UpperCaseJsonProvider implements IRequestBodyParserProvider {
        @Override
        public String getContentType() {
            return "APPLICATION/JSON";
        }

        @Override
        public IRequestBodyParser create(ConversionContext context) {
            return null;
        }
    }

}
