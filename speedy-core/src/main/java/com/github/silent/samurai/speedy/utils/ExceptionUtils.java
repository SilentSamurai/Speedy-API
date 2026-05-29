package com.github.silent.samurai.speedy.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;

public class ExceptionUtils {

    public static void writeException(HttpServletResponse response, SpeedyHttpException e) throws IOException {
        response.setStatus(e.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ObjectMapper jsPr = CommonUtil.json();
        ObjectNode basePayload = jsPr.createObjectNode();
        basePayload.put("status", e.getStatus());
        basePayload.put("message", e.getLocalizedMessage());
        basePayload.put("timestamp", LocalDateTime.now().toString());
        jsPr.writeValue(response.getWriter(), basePayload);
    }

    public static void writeException(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ObjectMapper jsPr = CommonUtil.json();
        ObjectNode basePayload = jsPr.createObjectNode();
        basePayload.put("status", status);
        basePayload.put("message", message);
        basePayload.put("timestamp", LocalDateTime.now().toString());
        jsPr.writeValue(response.getWriter(), basePayload);
    }
}
