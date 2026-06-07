package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.PayloadTooLargeException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/// Reads raw HTTP request data and populates transient fields on RequestContext.
///
/// Extracts the HTTP method, request URI, request headers, and raw body bytes
/// from HttpServletRequest. The raw bytes are stored for later parsing by
/// IRequestBodyParser implementations based on Content-Type.
///
/// @see IRequestBodyParser
public class RequestParserHandler implements Handler {

    private final long maxRequestBodySize;

    public RequestParserHandler(long maxRequestBodySize) {
        this.maxRequestBodySize = maxRequestBodySize;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        HttpServletRequest request = context.getHttpServletRequest();
        String requestURI = CommonUtil.getRequestURI(request);
        String method = request.getMethod();
        HttpMethod httpMethod = HttpMethod.valueOf(method);
        Map<String, String> headers = extractHeaders(request);

        context.setRequestUri(requestURI);
        context.setHttpMethod(httpMethod);
        context.setHeaders(headers);

        try {
            long contentLength = request.getContentLengthLong();
            if (maxRequestBodySize > 0 && contentLength > maxRequestBodySize) {
                throw new PayloadTooLargeException(
                        "Request body size " + contentLength + " exceeds maximum " + maxRequestBodySize + " bytes");
            }

            if (contentLength == 0) {
                context.setRawBody(new byte[0]);
            } else if (maxRequestBodySize > 0 && contentLength == -1) {
                InputStream is = request.getInputStream();
                int readLimit = maxRequestBodySize >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (maxRequestBodySize + 1);
                byte[] bodyBytes = is.readNBytes(readLimit);
                if (bodyBytes.length > maxRequestBodySize) {
                    throw new PayloadTooLargeException(
                            "Request body exceeds maximum " + maxRequestBodySize + " bytes");
                }
                context.setRawBody(bodyBytes);
            } else {
                context.setRawBody(request.getInputStream().readAllBytes());
            }
        } catch (IOException e) {
            throw new BadRequestException("Invalid Request", e);
        }

    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headers.put(name, request.getHeader(name));
            }
        }
        return Collections.unmodifiableMap(headers);
    }
}
