package com.github.silent.samurai.speedy.interfaces;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public interface RequestContext {
    MetaModelProcessor getMetaModelProcessor();

    HttpServletRequest getRequest();

    EntityMetadata getEntityMetadata();

    default String getRequestURI() throws UnsupportedEncodingException {
        String requestURI = URLDecoder.decode(getRequest().getRequestURI(), StandardCharsets.UTF_8);
        if (getRequest().getQueryString() != null) {
            requestURI += "?" + URLDecoder.decode(getRequest().getQueryString(), StandardCharsets.UTF_8);
        }
        return requestURI.replaceAll(SpeedyConstant.URI, "");
    }
}
