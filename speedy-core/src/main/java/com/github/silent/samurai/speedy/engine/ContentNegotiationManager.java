package com.github.silent.samurai.speedy.engine;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.ISpeedyIoProvider;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ContentNegotiationManager {

    public static final String DEFAULT_CONTENT_TYPE = "application/json";

    private final Map<String, ISpeedyIoProvider> providers;

    public ContentNegotiationManager(
            Map<String, ISpeedyIoProvider> providers) {
        this.providers = providers;
    }

    public ISpeedyIoProvider selectProvider(String contentType) throws SpeedyHttpException {
        ISpeedyIoProvider provider = findProvider(contentType);
        if (provider == null) {
            throw new InternalServerError("No ISpeedyIoProvider found on classpath");
        }
        warnIfUnsupported(contentType, provider.getContentType(), "Content-Type");
        return provider;
    }

    private void warnIfUnsupported(String requested, String selected, String headerName) {
        if (requested != null && !requested.isBlank()
                && !requested.contains("*/*")
                && !requested.toLowerCase().contains(selected.toLowerCase())) {
            log.warn("Unsupported {} header '{}', defaulting to {}", headerName, requested, selected);
        }
    }

    public IResponseSerializerV2 createDefaultSerializer(MetaModel metaModel, ConversionContext ctx) {
        ISpeedyIoProvider baseline = providers.get(DEFAULT_CONTENT_TYPE);
        if (baseline == null) {
            throw new SpeedyHttpRuntimeException(500,
                    "No ISpeedyIoProvider registered for '" + DEFAULT_CONTENT_TYPE + "'");
        }
        return baseline.createSerializer(metaModel, ctx);
    }

    private ISpeedyIoProvider findProvider(String contentType) {
        if (contentType == null || contentType.isBlank() || contentType.contains("*/*")) {
            return providers.get(DEFAULT_CONTENT_TYPE);
        }
        String normalized = contentType.toLowerCase();
        for (Map.Entry<String, ISpeedyIoProvider> entry : providers.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return providers.get(DEFAULT_CONTENT_TYPE);
    }
}
