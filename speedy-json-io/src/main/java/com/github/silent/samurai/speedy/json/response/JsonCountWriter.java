package com.github.silent.samurai.speedy.json.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.models.SpeedyCountResponse;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletResponse;

/// Writes a count result as JSON. One concern: {@code COUNT} responses.
public class JsonCountWriter {

    public void write(SpeedyCountResponse countResponse, HttpServletResponse httpResponse) throws SpeedyHttpException {
        ObjectMapper json = CommonUtil.json();
        ObjectNode basePayload = json.createObjectNode();
        basePayload.set("count", json.valueToTree(countResponse.getCount()));
        JsonHttpWriter.writeJson(countResponse, basePayload, httpResponse);
    }
}
