package com.github.silent.samurai.speedy.interfaces;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

public interface IResponseContext {

    MetaModelProcessor getMetaModelProcessor();

    HttpServletRequest getRequest();

    EntityMetadata getEntityMetadata();

    int getSerializationType();

    HttpServletResponse getResponse();

    int getPageNo();

    Set<String> getExpand();

}
