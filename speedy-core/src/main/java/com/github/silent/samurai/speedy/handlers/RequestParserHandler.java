package com.github.silent.samurai.speedy.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;

import java.io.IOException;

public class RequestParserHandler implements Handler {

    private final Handler next;

    public RequestParserHandler(final Handler next) {
        this.next = next;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        HttpServletRequest request = context.getHttpServletRequest();
        String requestURI = CommonUtil.getRequestURI(request);
        String method = request.getMethod();
        context.setRequestUri(requestURI);
        HttpMethod httpMethod = HttpMethod.valueOf(method);
        context.setHttpMethod(httpMethod);

        try {
            ObjectMapper json = CommonUtil.json();
            JsonNode jsonBody = json.readTree(context.getHttpServletRequest().getReader());
            context.setBody(jsonBody);
        } catch (IOException e) {
            throw new BadRequestException("Invalid Request", e);
        }

        next.process(context);
    }

}
