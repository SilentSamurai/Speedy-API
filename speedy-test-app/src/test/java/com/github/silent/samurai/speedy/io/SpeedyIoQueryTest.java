package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The structured {@code $query} POST body ({@code {"$from": ..., "$where": {...}}}) exercised
/// once per {@link IoFormat}. Unlike the URL GET reads in the CRUD suite, this drives the query
/// DSL through the request reader's body-parse path, then asserts the response envelope in the
/// negotiated format.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyIoQueryTest {

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest(name = "{0}")
    @EnumSource(IoFormat.class)
    void queryByWhereClause(IoFormat fmt) throws Exception {
        IoMvc io = new IoMvc(mvc, fmt);
        String name = "io-query-" + RandomString.make(8);

        // Seed a row to match on.
        ArrayNode createBody = CommonUtil.json().createArrayNode();
        createBody.addObject().put("name", name);
        io.post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix(), createBody);

        // Query it back through the structured $query body.
        ObjectNode query = CommonUtil.json().createObjectNode();
        query.put("$from", "Category");
        query.putObject("$where").put("name", name);

        MvcResult result = io.post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.QUERY.suffix(), query);
        JsonNode payload = io.tree(result).get("payload");

        assertEquals(1, payload.size());
        assertEquals(name, payload.get(0).get("name").asText());
    }
}
