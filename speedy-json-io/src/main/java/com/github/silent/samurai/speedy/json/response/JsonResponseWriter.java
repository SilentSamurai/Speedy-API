package com.github.silent.samurai.speedy.json.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponseWriter;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/// JSON {@link SpeedyResponseWriter}. Realizes the format-agnostic token stream as a
/// buffered Jackson tree, then serializes it to the {@link HttpServletResponse} in
/// {@link #finish}. Buffering into a tree (rather than streaming straight to the socket)
/// keeps the byte-for-byte output identical to the previous {@code ObjectNode}-based writers
/// and preserves transactional error handling — nothing is committed until the walk succeeds.
public class JsonResponseWriter implements SpeedyResponseWriter {

    private static final ObjectMapper JSON = CommonUtil.json();
    private static final JsonNodeFactory NF = JSON.getNodeFactory();
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ISO_TIME;
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final Deque<JsonNode> stack = new ArrayDeque<>();

    private JsonNode root;
    private String pendingField;

    public JsonResponseWriter() {
    }

    @Override
    public void startObject() {
        open(NF.objectNode());
    }

    @Override
    public void endObject() {
        stack.pop();
    }

    @Override
    public void startArray() {
        open(NF.arrayNode());
    }

    @Override
    public void endArray() {
        stack.pop();
    }

    @Override
    public void field(String name) {
        this.pendingField = name;
    }

    @Override
    public void writeNull() {
        add(NF.nullNode());
    }

    @Override
    public void writeSpeedyInt(SpeedyInt value) {
        add(NF.numberNode(value.getValue()));
    }

    @Override
    public void writeSpeedyText(SpeedyText value) {
        add(NF.textNode(value.getValue()));
    }

    @Override
    public void writeSpeedyDouble(SpeedyDouble value) {
        add(NF.numberNode(value.getValue()));
    }

    @Override
    public void writeSpeedyBoolean(SpeedyBoolean value) {
        add(NF.booleanNode(value.getValue()));
    }

    @Override
    public void writeSpeedyDate(SpeedyDate value) {
        add(NF.textNode(value.getValue().format(ISO_DATE)));
    }

    @Override
    public void writeSpeedyDateTime(SpeedyDateTime value) {
        add(NF.textNode(value.getValue().format(ISO_DATE_TIME)));
    }

    @Override
    public void writeSpeedyTime(SpeedyTime value) {
        add(NF.textNode(value.getValue().format(ISO_TIME)));
    }

    @Override
    public void writeSpeedyZonedDateTime(SpeedyZonedDateTime value) {
        add(NF.textNode(value.getValue().format(ISO_OFFSET_DATE_TIME)));
    }

    @Override
    public void writeSpeedyEnum(SpeedyEnum value) {
        if (value.getValueType() == ValueType.ENUM_ORD) {
            add(NF.numberNode(value.asEnumOrd()));
        } else {
            add(NF.textNode(value.asEnum()));
        }
    }

    @Override
    public void finish(HttpServletResponse out, int status, Map<String, String> headers, String contentType)
            throws SpeedyHttpException {
        out.setStatus(status);
        out.setContentType(contentType);
        headers.forEach(out::setHeader);
        try {
            JSON.writeValue(out.getWriter(), root);
        } catch (IOException e) {
            throw new InternalServerError("Internal Server Error", e);
        }
    }

    /// Attaches a freshly opened container to its parent and makes it the current container.
    private void open(JsonNode container) {
        add(container);
        stack.push(container);
    }

    /// Places a value/container into the enclosing object (under the pending field) or array,
    /// or records it as the document root when no container is open.
    private void add(JsonNode node) {
        JsonNode top = stack.peek();
        if (top == null) {
            root = node;
        } else if (top instanceof ObjectNode object) {
            object.set(pendingField, node);
            pendingField = null;
        } else if (top instanceof ArrayNode array) {
            array.add(node);
        }
    }
}
