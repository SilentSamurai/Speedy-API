package com.github.silent.samurai.speedy.serializers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.io.SelectiveSpeedy2Json;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.responses.MultiPayloadWrapper;
import com.github.silent.samurai.speedy.responses.SinglePayloadWrapper;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

public class JSONSerializer implements IResponseSerializer {

    private final IResponseContext context;
    private final Predicate<FieldMetadata> fieldPredicate;

    public JSONSerializer(IResponseContext context) {
        this.context = context;
        this.fieldPredicate = fieldMetadata -> true;
    }

    public JSONSerializer(IResponseContext context, Predicate<FieldMetadata> fieldPredicate) {
        this.context = context;
        this.fieldPredicate = fieldPredicate;
    }

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public IResponseContext getContext() {
        return context;
    }

    private void commonCode(JsonNode jsonElement, long pageIndex, long pageSize, long totalPageCount) throws SpeedyHttpException {
        ObjectMapper json = CommonUtil.json();
        ObjectNode basePayload = json.createObjectNode();
        basePayload.set("payload", jsonElement);
        basePayload.put("pageIndex", pageIndex);
        basePayload.put("pageSize", pageSize);
        basePayload.put("totalPageCount", totalPageCount);
        try {
            json.writeValue(context.getResponse().getWriter(), basePayload);
        } catch (IOException e) {
            throw new InternalServerError("Internal Server Error",e);
        }
    }

    public void writeResponse(SinglePayload requestedPayload) throws SpeedyHttpException {
        SelectiveSpeedy2Json selectiveSpeedy2Json = new SelectiveSpeedy2Json(
                context.getMetaModel(), fieldPredicate);

        SpeedyEntity payload = (SpeedyEntity) requestedPayload.getPayload();
        JsonNode jsonElement = selectiveSpeedy2Json.fromSpeedyEntity(payload, context.getEntityMetadata(), context.getExpand());
        commonCode(jsonElement, requestedPayload.getPageIndex(), requestedPayload.getPageSize(), requestedPayload.getTotalPageCount());
    }

    public void writeResponse(MultiPayload multiPayload) throws SpeedyHttpException {
        SelectiveSpeedy2Json selectiveSpeedy2Json = new SelectiveSpeedy2Json(
                context.getMetaModel(),
                fieldPredicate
        );
        List<? extends SpeedyValue> resultList = multiPayload.getPayload();
        JsonNode jsonElement = selectiveSpeedy2Json.formCollection(
                resultList,
                context.getEntityMetadata(),
                context.getExpand()
        );
        commonCode(jsonElement, multiPayload.getPageIndex(), multiPayload.getPageSize(), multiPayload.getTotalPageCount());
    }

    @Override
    public void write(List<SpeedyEntity> speedyEntities) throws SpeedyHttpException {
        MultiPayloadWrapper responseWrapper = MultiPayloadWrapper.wrapperInResponse(speedyEntities);
        responseWrapper.setPageIndex(getContext().getPageNo());
//        responseWrapper.setPageSize(getContext().getPageSize());
        HttpServletResponse response = getContext().getResponse();
        response.setContentType(this.getContentType());
        response.setStatus(HttpServletResponse.SC_OK);
        this.writeResponse(responseWrapper);
    }

    @Override
    public void write(SpeedyEntity speedyEntity) throws SpeedyHttpException {
        SinglePayloadWrapper responseWrapper = SinglePayloadWrapper.wrapperInResponse(speedyEntity);
        responseWrapper.setPageIndex(getContext().getPageNo());
//        responseWrapper.setPageSize(getContext().getPageSize());
        HttpServletResponse response = getContext().getResponse();
        response.setContentType(this.getContentType());
        response.setStatus(HttpServletResponse.SC_OK);
        this.writeResponse(responseWrapper);
    }

    @Override
    public void write(BigInteger count) throws SpeedyHttpException {
        try {
            HttpServletResponse response = getContext().getResponse();
            response.setContentType(this.getContentType());
            response.setStatus(HttpServletResponse.SC_OK);
            ObjectMapper json = CommonUtil.json();
            ObjectNode basePayload = json.createObjectNode();
            basePayload.set("count", json.valueToTree(count));
            json.writeValue(context.getResponse().getWriter(), basePayload);
        } catch (IOException e) {
            throw new InternalServerError("Internal Server Error",e);
        }
    }


}
