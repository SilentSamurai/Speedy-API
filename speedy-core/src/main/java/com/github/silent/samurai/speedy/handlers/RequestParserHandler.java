package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.PayloadTooLargeException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/// Reads raw HTTP request data and populates transient fields on SpeedyContext.
///
/// Extracts the HTTP method, request URI, request headers, and raw body bytes
/// from HttpServletRequest. The raw bytes are stored for later parsing by
/// IRequestBodyParser implementations based on Content-Type.
///
/// @see IRequestBodyParser
public class RequestParserHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    private final long maxRequestBodySize;

    public RequestParserHandler(long maxRequestBodySize) {
        this.maxRequestBodySize = maxRequestBodySize;
    }

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        HttpServletRequest request = context.get(HttpServletRequest.class);
        String method = request.getMethod();
        HttpMethod httpMethod = HttpMethod.valueOf(method);
        SpeedyHeaders headers = new SpeedyHeaders(extractHeaders(request));

        context.put(httpMethod);
        context.put(headers);

        try {
            long contentLength = request.getContentLengthLong();
            if (maxRequestBodySize > 0 && contentLength > maxRequestBodySize) {
                throw new PayloadTooLargeException(
                        "Request body size " + contentLength + " exceeds maximum " + maxRequestBodySize + " bytes");
            }

            if (contentLength == 0) {
                context.put(new byte[0]);
            } else if (contentLength == -1) {
                // Chunked transfer encoding (unknown length). Always bound the read to prevent OOM.
                long effectiveLimit = maxRequestBodySize > 0 ? maxRequestBodySize : Integer.MAX_VALUE;
                InputStream is = request.getInputStream();
                int readLimit = effectiveLimit >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (effectiveLimit + 1);
                byte[] bodyBytes = is.readNBytes(readLimit);
                if (maxRequestBodySize > 0 && bodyBytes.length > maxRequestBodySize) {
                    throw new PayloadTooLargeException(
                            "Request body exceeds maximum " + maxRequestBodySize + " bytes");
                }
                context.put(bodyBytes);
            } else {
                // contentLength > 0 — already checked against maxRequestBodySize above
                context.put(request.getInputStream().readNBytes((int) contentLength));
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
