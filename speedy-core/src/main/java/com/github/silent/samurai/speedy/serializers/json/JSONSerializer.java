package com.github.silent.samurai.speedy.serializers.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.serializers.SelectiveFieldJsonSerializer;
import com.github.silent.samurai.speedy.utils.CommonUtil;
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
            List<SpeedyValue> resultList = (List<SpeedyValue>) requestedPayload.getPayload();
            jsonElement = selectiveFieldJsonSerializer.formCollection(resultList, context.getEntityMetadata(), context.getSerializationType(), 0);
        } else {
            SpeedyEntity payload = (SpeedyEntity) requestedPayload.getPayload();
            jsonElement = selectiveFieldJsonSerializer.fromSpeedyEntity(payload, context.getEntityMetadata(), context.getSerializationType(), 0);
        }
        ObjectMapper json = CommonUtil.json();
        ObjectNode basePayload = json.createObjectNode();
        basePayload.set("payload", jsonElement);
        basePayload.put("pageIndex", requestedPayload.getPageIndex());
        basePayload.put("pageCount", requestedPayload.getPageCount());
        json.writeValue(context.getResponse().getWriter(), basePayload);
    }


}
