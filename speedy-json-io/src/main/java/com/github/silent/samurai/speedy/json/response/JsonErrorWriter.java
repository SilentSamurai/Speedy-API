package com.github.silent.samurai.speedy.json.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.models.SpeedyErrorResponse;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;

/// Writes a server-level error document as JSON. One concern: {@code ERROR} responses.
public class JsonErrorWriter {

    public void write(SpeedyErrorResponse errorResponse, HttpServletResponse httpResponse) throws SpeedyHttpException {
        ObjectMapper json = CommonUtil.json();
        ObjectNode payload = json.createObjectNode();
        payload.put("status", errorResponse.getStatus());
        payload.put("message", errorResponse.getMessage());
        payload.put("timestamp", LocalDateTime.now().toString());
        JsonHttpWriter.writeJson(errorResponse, payload, httpResponse);
    }
}
