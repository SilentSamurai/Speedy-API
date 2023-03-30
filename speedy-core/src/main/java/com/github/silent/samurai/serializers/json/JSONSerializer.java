package com.github.silent.samurai.serializers.json;

import com.github.silent.samurai.interfaces.IBaseResponsePayload;
import com.github.silent.samurai.interfaces.IResponseSerializer;
import com.github.silent.samurai.interfaces.ResponseReturningRequestContext;
import com.github.silent.samurai.serializers.ApiAutomateJsonSerializer;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class JSONSerializer implements IResponseSerializer {

    private final ResponseReturningRequestContext context;

    public JSONSerializer(ResponseReturningRequestContext context) {
        this.context = context;
    }

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    public void writeResponse(IBaseResponsePayload requestedPayload, HttpServletResponse response) throws Exception {
        JsonElement jsonElement;
        ApiAutomateJsonSerializer apiAutomateJsonSerializer = new ApiAutomateJsonSerializer(context.getMetaModelProcessor());
        if (context.getSerializationType() == IResponseSerializer.MULTIPLE_ENTITY) {
            List<?> resultList = (List<?>) requestedPayload.getPayload();
            jsonElement = apiAutomateJsonSerializer.formCollection(resultList, context.getSerializationType());
        } else {
            jsonElement = apiAutomateJsonSerializer.fromObject(requestedPayload.getPayload(), requestedPayload.getPayload().getClass(), context.getSerializationType());
        }
        JsonObject basePayload = new JsonObject();
        basePayload.add("payload", jsonElement);
        basePayload.addProperty("pageIndex", requestedPayload.getPageIndex());
        basePayload.addProperty("pageCount", requestedPayload.getPageCount());

        Gson gson = CommonUtil.getGson();
        gson.toJson(basePayload, response.getWriter());
    }


}
