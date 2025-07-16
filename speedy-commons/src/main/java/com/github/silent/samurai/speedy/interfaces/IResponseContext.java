package com.github.silent.samurai.speedy.interfaces;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Set;

public interface IResponseContext {

    MetaModel getMetaModel();

    HttpServletRequest getRequest();

    EntityMetadata getEntityMetadata();

    int getSerializationType();

    HttpServletResponse getResponse();

    int getPageNo();

    Set<String> getExpand();

}
