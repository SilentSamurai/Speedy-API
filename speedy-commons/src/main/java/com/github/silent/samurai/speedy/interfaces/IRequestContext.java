package com.github.silent.samurai.speedy.interfaces;

import jakarta.servlet.http.HttpServletRequest;

public interface IRequestContext {

    MetaModelProcessor getMetaModelProcessor();

    HttpServletRequest getRequest();

    EntityMetadata getEntityMetadata();


}
