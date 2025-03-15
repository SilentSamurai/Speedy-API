package com.github.silent.samurai.speedy.interfaces;

import jakarta.servlet.http.HttpServletRequest;

public interface IRequestContext {

    MetaModel getMetaModel();

    HttpServletRequest getRequest();

    EntityMetadata getEntityMetadata();


}
