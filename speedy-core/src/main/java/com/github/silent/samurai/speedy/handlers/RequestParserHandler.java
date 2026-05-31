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

/// # RequestParserHandler
///
/// Extracts the HTTP method, request URI, and JSON body from the
/// {@code HttpServletRequest} and populates them on the {@link RequestContext}.
/// Enforces a configurable maximum request body size before parsing.
///
/// ## Purpose
/// - Reads and normalizes the request URI
/// - Parses the HTTP method into a Spring {@code HttpMethod} enum
/// - Parses the request body into a Jackson {@code JsonNode}, enforcing size limits
///
/// ## Processing Flow
/// 1. Extracts the URI and HTTP method from the servlet request
/// 2. Checks Content-Length against the configured max body size
/// 3. Parses the body stream into a {@code JsonNode}
///    (with safe read limit for unknown content length)
/// 4. Sets {@code requestUri}, {@code httpMethod}, and {@code body} on the context
/// 5. Delegates to the next handler
///
/// ## Chain Position
/// Second handler in the chain, immediately after {@link HeadHandler}.
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
