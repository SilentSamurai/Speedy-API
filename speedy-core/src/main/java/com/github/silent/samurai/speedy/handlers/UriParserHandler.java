package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.conversion.registry.JavaTypeRegistry;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import jakarta.servlet.http.HttpServletRequest;
import com.github.silent.samurai.speedy.parser.SpeedyUriComponents;

public class UriParserHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        MetaModel metaModel = context.get(MetaModel.class);
        HttpServletRequest httpRequest = context.get(HttpServletRequest.class);
        String requestURI = getRequestURI(httpRequest);
        JavaTypeRegistry jtr = context.get(ConversionContext.class).get(JavaTypeRegistry.class);

        java.util.List<String> pathSegments = SpeedyUriComponents.parse(requestURI).getPathSegments();

        // Server-level actions (e.g. /$metadata) have no entity to resolve
        if (pathSegments.size() == 1 && "$metadata".equals(pathSegments.get(0))) {
            SpeedyUriContext ctx = SpeedyUriContext.builder()
                    .metaModel(metaModel)
                    .requestURI(requestURI)
                    .javaTypeRegistry(jtr)
                    .build();
            ctx.setActionSuffix(pathSegments.get(0));
            context.put(ctx);
            return;
        }

        SpeedyUriContext parser = SpeedyUriContext.builder()
                .metaModel(metaModel)
                .requestURI(requestURI)
                .maxPageSize(context.get(ISpeedyConfiguration.class).getMaxPageSize())
                .defaultPageSize(context.get(ISpeedyConfiguration.class).getDefaultPageSize())
                .maxQueryStringLength(context.get(ISpeedyConfiguration.class).getMaxQueryStringLength())
                .maxFilterCount(context.get(ISpeedyConfiguration.class).getMaxFilterCount())
                .javaTypeRegistry(jtr)
                .build();
        parser.parse();

        context.put(parser);
    }

    private static String getRequestURI(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        if (request.getQueryString() != null) {
            requestURI += "?" + request.getQueryString();
        }
        return requestURI.replaceFirst("^" + SpeedyConstants.URI, "");
    }
}
