package com.github.silent.samurai.speedy.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IResponseContext;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.math.BigInteger;

public class JSONCountSerializerV2 implements IResponseSerializerV2 {

    private final BigInteger count;

    public JSONCountSerializerV2(BigInteger count) {
        this.count = count;
    }

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public void write(IResponseContext context) throws SpeedyHttpException {
        try {
            HttpServletResponse response = context.getResponse();
            response.setContentType(this.getContentType());
            response.setStatus(HttpServletResponse.SC_OK);
            ObjectMapper json = CommonUtil.json();
            ObjectNode basePayload = json.createObjectNode();
            basePayload.set("count", json.valueToTree(count));
            json.writeValue(context.getResponse().getWriter(), basePayload);
        } catch (IOException e) {
            throw new InternalServerError("Internal Server Error", e);
        }
    }

}
