package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.silent.samurai.speedy.enums.SpeedyEndpoint.QUERY;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Integration tests for {@code DefaultQueryValidator}: the per-condition operator/type rules
/// and the query-level complexity limits (condition count, nesting depth, expand count), exercised
/// over the real {@code $query} (POST) and {@code GET} endpoints.
///
/// Limits use the {@link com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration} defaults
/// active in this app: max filter count 100, max nesting depth 5, max expand count 10.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyV2QueryValidationTest {

    @Autowired
    MockMvc mvc;

    private void postQueryExpectingMessage(ObjectNode body, String message) throws Exception {
        mvc.perform(post(SpeedyConstant.URI + "/" + body.get("$from").asText() + "/" + QUERY.suffix())
                        .content(CommonUtil.json().writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(message)));
    }

    @Test
    @DisplayName("relational operator on a boolean field returns 400")
    void relationalOperatorOnBooleanField() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "ValueTestEntity");
        body.putObject("$where").putObject("booleanValue").put("$gt", true);

        postQueryExpectingMessage(body, "not supported on field");
    }

    @Test
    @DisplayName("more filter conditions than the configured maximum returns 400")
    void tooManyConditions() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        ArrayNode and = body.putObject("$where").putArray("$and");
        for (int i = 0; i < 101; i++) { // default max filter count is 100
            and.addObject().put("name", "x" + i);
        }

        postQueryExpectingMessage(body, "too many query conditions");
    }

    @Test
    @DisplayName("condition nesting deeper than the configured maximum returns 400")
    void tooDeeplyNested() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        // 6 nested $and wrappers -> boolean depth 7, exceeding the default max depth of 5
        ObjectNode current = body.putObject("$where");
        for (int i = 0; i < 6; i++) {
            ObjectNode child = CommonUtil.json().createObjectNode();
            current.putArray("$and").add(child);
            current = child;
        }
        current.put("name", "x");

        postQueryExpectingMessage(body, "too deep");
    }

    @Test
    @DisplayName("more $expand entries than the configured maximum returns 400")
    void tooManyExpands() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        ArrayNode expand = body.putArray("$expand");
        for (int i = 0; i < 11; i++) { // default max expand count is 10
            expand.add("rel" + i);
        }

        postQueryExpectingMessage(body, "$expand");
    }

    @Test
    @DisplayName("GET endpoint is also validated: too many $expand entries returns 400")
    void getEndpointEnforcesExpandLimit() throws Exception {
        // GET routes through the same validator as $query (GetHandler); 11 expands exceeds the max of 10.
        String expands = String.join(",", "r0", "r1", "r2", "r3", "r4", "r5", "r6", "r7", "r8", "r9", "r10");

        mvc.perform(get(SpeedyConstant.URI + "/Product?$expand=" + expands)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("$expand")));
    }

    @Test
    @DisplayName("a query within all limits still succeeds")
    void queryWithinLimitsSucceeds() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        body.putObject("$where").put("name", "cat-1-1");

        mvc.perform(post(SpeedyConstant.URI + "/Category/" + QUERY.suffix())
                        .content(CommonUtil.json().writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray());
    }
}
