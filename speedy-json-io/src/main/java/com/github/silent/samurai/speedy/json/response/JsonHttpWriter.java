package com.github.silent.samurai.speedy.json.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponse;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

/// Shared HTTP plumbing for the per-response JSON writers: stamps the status code,
/// {@code application/json} content type and headers from the {@link SpeedyResponse},
/// then writes the prepared body. Each writer only has to build its {@link JsonNode}.
final class JsonHttpWriter {

    private JsonHttpWriter() {
    }

    static void writeJson(SpeedyResponse response, JsonNode body, HttpServletResponse httpResponse)
            throws SpeedyHttpException {
        httpResponse.setStatus(response.getStatus());
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().forEach(httpResponse::setHeader);
        try {
            CommonUtil.json().writeValue(httpResponse.getWriter(), body);
        } catch (IOException e) {
            throw new InternalServerError("Internal Server Error", e);
        }
    }
}
