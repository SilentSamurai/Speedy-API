package com.github.silent.samurai.speedy.engine;

import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParserProvider;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ContentNegotiationManager {

    public static final String DEFAULT_CONTENT_TYPE = "application/json";

    private final Map<String, IRequestBodyParserProvider> parserProviders;
    private final Map<String, IResponseSerializerProvider> serializerProviders;

    public ContentNegotiationManager(
            Map<String, IRequestBodyParserProvider> parserProviders,
            Map<String, IResponseSerializerProvider> serializerProviders) {
        this.parserProviders = parserProviders;
        this.serializerProviders = serializerProviders;
    }

    public IRequestBodyParserProvider selectParser(String contentType) throws SpeedyHttpException {
        IRequestBodyParserProvider provider = findParser(contentType);
        if (provider == null) {
            throw new InternalServerError("No IRequestBodyParserProvider found on classpath");
        }
        warnIfUnsupported(contentType, provider.getContentType(), "Content-Type");
        return provider;
    }

    public IResponseSerializerProvider selectSerializer(String acceptHeader) throws SpeedyHttpException {
        IResponseSerializerProvider provider = findSerializer(acceptHeader);
        if (provider == null) {
            throw new InternalServerError("No IResponseSerializerProvider found on classpath");
        }
        warnIfUnsupported(acceptHeader, provider.getContentType(), "Accept");
        return provider;
    }

    private void warnIfUnsupported(String requested, String selected, String headerName) {
        if (requested != null && !requested.isBlank()
                && !requested.contains("*/*")
                && !requested.toLowerCase().contains(selected.toLowerCase())) {
            log.warn("Unsupported {} header '{}', defaulting to {}", headerName, requested, selected);
        }
    }

    private IRequestBodyParserProvider findParser(String contentType) {
        if (contentType == null || contentType.isBlank() || contentType.contains("*/*")) {
            return parserProviders.get(DEFAULT_CONTENT_TYPE);
        }
        String normalized = contentType.toLowerCase();
        for (Map.Entry<String, IRequestBodyParserProvider> entry : parserProviders.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return parserProviders.get(DEFAULT_CONTENT_TYPE);
    }

    private IResponseSerializerProvider findSerializer(String accept) {
        if (accept == null || accept.isBlank() || accept.contains("*/*")) {
            return serializerProviders.get(DEFAULT_CONTENT_TYPE);
        }
        String normalized = accept.toLowerCase();
        for (Map.Entry<String, IResponseSerializerProvider> entry : serializerProviders.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return serializerProviders.get(DEFAULT_CONTENT_TYPE);
    }
}
