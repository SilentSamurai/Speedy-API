package com.github.silent.samurai.interfaces;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public interface RequestContext {
    MetaModelProcessor getMetaModelProcessor();

    HttpServletRequest getRequest();

    EntityManager getEntityManager();

    default String getRequestURI() throws UnsupportedEncodingException {
        String requestURI = URLDecoder.decode(getRequest().getRequestURI() + "?" + getRequest().getQueryString(), StandardCharsets.UTF_8.name());
        return requestURI.replaceAll(SpeedyConstant.URI, "");
    }
}
