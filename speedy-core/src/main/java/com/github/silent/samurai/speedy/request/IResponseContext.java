package com.github.silent.samurai.speedy.request;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializer;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class IResponseContext implements com.github.silent.samurai.speedy.interfaces.IResponseContext {

    private final MetaModel metaModel;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final EntityMetadata entityMetadata;
    int serializationType = IResponseSerializer.MULTIPLE_ENTITY;
    int pageNo = 0;
    Set<String> expands = new HashSet<>();

    public IResponseContext(EntityMetadata entityMetadata,
                            HttpServletResponse response,
                            HttpServletRequest request,
                            MetaModel metaModel) {
        this.entityMetadata = entityMetadata;
        this.response = response;
        this.request = request;
        this.metaModel = metaModel;
    }

    public static ResponseContextBuilder builder() {
        return new ResponseContextBuilder();
    }

    @Override
    public int getSerializationType() {
        return serializationType;
    }

    @Override
    public int getPageNo() {
        return pageNo;
    }

    @Override
    public Set<String> getExpand() {
        return expands;
    }

    public static final class ResponseContextBuilder {
        private MetaModel metaModel;
        private HttpServletRequest request;
        private HttpServletResponse response;
        private EntityMetadata entityMetadata;
        private int serializationType = IResponseSerializer.MULTIPLE_ENTITY;
        private int pageNo = 0;
        private Set<String> expands = new HashSet<>();

        private ResponseContextBuilder() {
        }

        public static ResponseContextBuilder aResponseContext() {
            return new ResponseContextBuilder();
        }

        public ResponseContextBuilder metaModelProcessor(MetaModel metaModel) {
            this.metaModel = metaModel;
            return this;
        }

        public ResponseContextBuilder request(HttpServletRequest request) {
            this.request = request;
            return this;
        }

        public ResponseContextBuilder response(HttpServletResponse response) {
            this.response = response;
            return this;
        }

        public ResponseContextBuilder entityMetadata(EntityMetadata entityMetadata) {
            this.entityMetadata = entityMetadata;
            return this;
        }

        public ResponseContextBuilder serializationType(int serializationType) {
            this.serializationType = serializationType;
            return this;
        }

        public ResponseContextBuilder pageNo(int pageNo) {
            this.pageNo = pageNo;
            return this;
        }

        public ResponseContextBuilder expands(Set<String> expands) {
            this.expands = expands;
            return this;
        }

        public IResponseContext build() {
            IResponseContext responseContext = new IResponseContext(entityMetadata, response, request, metaModel);
            responseContext.serializationType = this.serializationType;
            responseContext.pageNo = this.pageNo;
            responseContext.expands = this.expands;
            return responseContext;
        }
    }
}
