package com.github.silent.samurai.speedy.serializers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.io.SelectiveSpeedy2Json;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.responses.MultiPayloadWrapper;
import com.github.silent.samurai.speedy.responses.SinglePayloadWrapper;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    public void writeResponse(SinglePayload requestedPayload) throws Exception {
        SelectiveSpeedy2Json selectiveSpeedy2Json = new SelectiveSpeedy2Json(
                context.getMetaModelProcessor(), fieldPredicate);
        SpeedyEntity payload = (SpeedyEntity) requestedPayload.getPayload();
        JsonNode jsonElement = selectiveSpeedy2Json.fromSpeedyEntity(payload, context.getEntityMetadata());
        commonCode(jsonElement, requestedPayload.getPageIndex(), requestedPayload.getPageSize(), requestedPayload.getTotalPageCount());
    }

    private void commonCode(JsonNode jsonElement, long pageIndex, long pageSize, long totalPageCount) throws IOException {
        ObjectMapper json = CommonUtil.json();
        ObjectNode basePayload = json.createObjectNode();
        basePayload.set("payload", jsonElement);
        basePayload.put("pageIndex", pageIndex);
        basePayload.put("pageSize", pageSize);
        basePayload.put("totalPageCount", totalPageCount);
        json.writeValue(context.getResponse().getWriter(), basePayload);
    }

    public void writeResponse(MultiPayload multiPayload) throws Exception {
        SelectiveSpeedy2Json selectiveSpeedy2Json = new SelectiveSpeedy2Json(context.getMetaModelProcessor(), fieldPredicate);
        List<? extends SpeedyValue> resultList = multiPayload.getPayload();
        JsonNode jsonElement = selectiveSpeedy2Json.formCollection(resultList, context.getEntityMetadata());
        commonCode(jsonElement, multiPayload.getPageIndex(), multiPayload.getPageSize(), multiPayload.getTotalPageCount());
    }

    @Override
    public void write(List<SpeedyEntity> speedyEntities) throws Exception {
        MultiPayloadWrapper responseWrapper = MultiPayloadWrapper.wrapperInResponse(speedyEntities);
        int pageNumber = getContext().getQuery().getPageInfo().getPageNo();
        responseWrapper.setPageIndex(pageNumber);
        getContext().getResponse().setContentType(this.getContentType());
        getContext().getResponse().setStatus(HttpServletResponse.SC_OK);
        this.writeResponse(responseWrapper);
    }

    @Override
    public void write(SpeedyEntity speedyEntity) throws Exception {
        SinglePayloadWrapper responseWrapper = SinglePayloadWrapper.wrapperInResponse(speedyEntity);
        getContext().getResponse().setContentType(this.getContentType());
        getContext().getResponse().setStatus(HttpServletResponse.SC_OK);
        this.writeResponse(responseWrapper);
    }


}
