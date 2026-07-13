package com.github.silent.samurai.speedy.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.github.silent.samurai.speedy.policy.PolicyBuilder.denyByDefault;
import static com.github.silent.samurai.speedy.policy.PolicyConditions.fieldEquals;
import static com.github.silent.samurai.speedy.policy.PolicyConditions.variable;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Exercises row-level ABAC scoped to an explicit {@code owner} column (see {@link
/// com.github.silent.samurai.speedy.entity.Document}), as opposed to the other policy tests that
/// repurpose an existing field (e.g. {@code Category.name}) as a stand-in for ownership.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PolicyTestConfiguration.class)
class OwnerScopedRowPolicyTest {

    @Autowired
    private MockMvc mvc;

    private static String operationUrl(String entity, SpeedyEndpoint endpoint) {
        return SpeedyConstants.URI + "/" + entity + "/" + endpoint.suffix();
    }

    private static JsonNode responseBody(MvcResult result) throws Exception {
        return CommonUtil.json().readTree(result.getResponse().getContentAsString());
    }

    private static RequestPostProcessor withPolicy(SpeedyAuthContext authContext) {
        return request -> {
            request.setAttribute(PolicyTestConfiguration.AUTH_CONTEXT_ATTRIBUTE, authContext);
            return request;
        };
    }

    /// A row-scoped read rule keyed on an explicit {@code owner} field excludes rows another
    /// principal created, not merely rows that happen not to match by coincidence: a row owned by
    /// a different principal is deliberately created first and asserted absent from the response.
    @Test
    void principalOnlySeesRowsTheyOwnNotRowsOwnedByOtherPrincipals() throws Exception {
        String ownRowId = createScratchDocument("own-document", "principal-a");
        String otherOwnerRowId = createScratchDocument("other-document", "principal-b");

        SpeedyAuthContext authContext = denyByDefault()
                .principalId("principal-a")
                .allow("read-own-documents", PermissionType.READ, "Document.*",
                        fieldEquals("owner", variable("principal.id")))
                .build();

        MvcResult result = mvc.perform(get(SpeedyConstants.URI + "/Document")
                        .with(withPolicy(authContext))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(1))
                .andExpect(jsonPath("$.payload[0].id").value(ownRowId))
                .andExpect(jsonPath("$.payload[0].owner").value("principal-a"))
                .andReturn();

        JsonNode payload = responseBody(result).path("payload");
        for (JsonNode document : payload) {
            assertNotEquals(otherOwnerRowId, document.path("id").asText());
        }
    }

    /// Creates a throwaway Document with an explicit creation policy, so this test doesn't need to
    /// depend on shared seed data.
    private String createScratchDocument(String title, String owner) throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .allow("scratch-create", PermissionType.CREATE, "Document.*")
                .build();

        MvcResult result = mvc.perform(post(operationUrl("Document", SpeedyEndpoint.CREATE))
                        .with(withPolicy(authContext))
                        .content("[{\"title\":\"" + title + "\",\"owner\":\"" + owner + "\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return responseBody(result).path("payload").get(0).path("id").asText();
    }

}
