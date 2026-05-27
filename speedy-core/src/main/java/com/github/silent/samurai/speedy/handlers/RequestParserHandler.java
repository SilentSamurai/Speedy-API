package com.github.silent.samurai.speedy.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.PayloadTooLargeException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.io.InputStream;

public class RequestParserHandler implements Handler {

    private final Handler next;
    private final long maxRequestBodySize;

    public RequestParserHandler(final Handler next, long maxRequestBodySize) {
        this.next = next;
        this.maxRequestBodySize = maxRequestBodySize;
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
            long contentLength = request.getContentLengthLong();
            if (maxRequestBodySize > 0 && contentLength > maxRequestBodySize) {
                throw new PayloadTooLargeException(
                        "Request body size " + contentLength + " exceeds maximum " + maxRequestBodySize + " bytes");
            }

            ObjectMapper json = CommonUtil.json();
            JsonNode jsonBody;

            if (maxRequestBodySize > 0 && contentLength == -1) {
                InputStream is = request.getInputStream();
                int readLimit = maxRequestBodySize >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (maxRequestBodySize + 1);
                byte[] bodyBytes = is.readNBytes(readLimit);
                if (bodyBytes.length > maxRequestBodySize) {
                    throw new PayloadTooLargeException(
                            "Request body exceeds maximum " + maxRequestBodySize + " bytes");
                }
                jsonBody = json.readTree(bodyBytes);
            } else {
                jsonBody = json.readTree(request.getReader());
            }

            context.setBody(jsonBody);
        } catch (IOException e) {
            throw new BadRequestException("Invalid Request", e);
        }

        next.process(context);
    }

}
