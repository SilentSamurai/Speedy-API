package com.github.silent.samurai.serializers.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.IBaseResponsePayload;
import com.github.silent.samurai.interfaces.IResponseSerializer;
import com.github.silent.samurai.interfaces.ResponseReturningRequestContext;
import com.github.silent.samurai.serializers.SelectiveFieldJsonSerializer;
import com.github.silent.samurai.utils.CommonUtil;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.function.Predicate;

public class JSONSerializer implements IResponseSerializer {

    private final ResponseReturningRequestContext context;
    private final Predicate<FieldMetadata> fieldPredicate;

    public JSONSerializer(ResponseReturningRequestContext context) {
        this.context = context;
        this.fieldPredicate = fieldMetadata -> true;
    }

    public JSONSerializer(ResponseReturningRequestContext context, Predicate<FieldMetadata> fieldPredicate) {
        this.context = context;
        this.fieldPredicate = fieldPredicate;
    }

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public ResponseReturningRequestContext getContext() {
        return context;
    }

    public void writeResponse(IBaseResponsePayload requestedPayload) throws Exception {
        JsonNode jsonElement;
        SelectiveFieldJsonSerializer selectiveFieldJsonSerializer = new SelectiveFieldJsonSerializer(context.getMetaModelProcessor(), fieldPredicate);
        if (context.getSerializationType() == IResponseSerializer.MULTIPLE_ENTITY) {
            List<?> resultList = (List<?>) requestedPayload.getPayload();
            jsonElement = selectiveFieldJsonSerializer.formCollection(resultList, context.getSerializationType());
        } else {
            jsonElement = selectiveFieldJsonSerializer.fromObject(requestedPayload.getPayload(), requestedPayload.getPayload().getClass(), context.getSerializationType());
        }
        ObjectMapper json = CommonUtil.json();
        ObjectNode basePayload = json.createObjectNode();
        basePayload.set("payload", jsonElement);
        basePayload.put("pageIndex", requestedPayload.getPageIndex());
        basePayload.put("pageCount", requestedPayload.getPageCount());

        json.writeValue(context.getResponse().getWriter(), basePayload);
    }


}
