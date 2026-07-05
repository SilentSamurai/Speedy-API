package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// The error envelope ({@code status}/{@code message}/{@code timestamp}) rendered once per
/// {@link IoFormat}. Uses validation failures on a valid entity (empty and over-long name),
/// which occur AFTER content negotiation, so the error body is written by the negotiated
/// writer — proving errors are not silently forced to JSON. (Pre-negotiation failures such as
/// an unknown entity intentionally fall back to JSON and are out of scope here.)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyIoErrorTest {

    @Autowired
    private MockMvc mvc;

    private static ArrayNode oneCategory(String name) {
        ArrayNode body = CommonUtil.json().createArrayNode();
        body.addObject().put("name", name);
        return body;
    }

    /// Asserts the standard error envelope, parsed in the negotiated format.
    private static void assertErrorEnvelope(IoMvc io, MvcResult result, int expectedStatus) {
        JsonNode error = io.tree(result); // also asserts the negotiated content type
        assertEquals(expectedStatus, error.get("status").asInt());
        assertNotNull(error.get("message"), "error had no message");
        assertFalse(error.get("message").asText().isEmpty(), "error message was empty");
        assertNotNull(error.get("timestamp"), "error had no timestamp");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(IoFormat.class)
    void emptyName_returns400Envelope(IoFormat fmt) throws Exception {
        IoMvc io = new IoMvc(mvc, fmt);
        MvcResult result = io.post(
                SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix(),
                oneCategory(""), 400);
        assertErrorEnvelope(io, result, 400);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(IoFormat.class)
    void tooLongName_returns400Envelope(IoFormat fmt) throws Exception {
        IoMvc io = new IoMvc(mvc, fmt);
        MvcResult result = io.post(
                SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix(),
                oneCategory(RandomString.make(251)), 400);
        assertErrorEnvelope(io, result, 400);
    }
}
