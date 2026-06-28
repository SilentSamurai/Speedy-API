package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class ExceptionMappingTest {

    @Autowired
    private MockMvc mockMvc;

    // -- US2: Custom Exception Handlers --

    @Test
    void customHandler_parameterized_returnsCustomStatusAndMessage() throws Exception {
        String categoryName = "test-cat-" + java.util.UUID.randomUUID();
        String catJson = "[{\"name\":\"" + categoryName + "\"}]";

        String catResponse = mockMvc.perform(post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix())
                        .content(catJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String catId = com.jayway.jsonpath.JsonPath.read(catResponse, "$.payload[0].id");

        String productJson = "[{\"name\":\"throw-business-exception\",\"category\":{\"id\":\"" + catId + "\"}}]";

        mockMvc.perform(post(SpeedyConstants.URI + "/Product/" + SpeedyEndpoint.CREATE.suffix())
                        .content(productJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Business rule violated: Test business error for exception mapping"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void customHandler_noParameter_returnsStaticMessage() throws Exception {
        String categoryName = "test-cat-" + java.util.UUID.randomUUID();
        String catJson = "[{\"name\":\"" + categoryName + "\"}]";

        String catResponse = mockMvc.perform(post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix())
                        .content(catJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String catId = com.jayway.jsonpath.JsonPath.read(catResponse, "$.payload[0].id");

        String productJson = "[{\"name\":\"throw-illegal-state\",\"category\":{\"id\":\"" + catId + "\"}}]";

        mockMvc.perform(post(SpeedyConstants.URI + "/Product/" + SpeedyEndpoint.CREATE.suffix())
                        .content(productJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(409))
                .andExpect(jsonPath("$.message").value("Resource conflict detected"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void customHandler_nestedException_picksInnermostHandler() throws Exception {
        String categoryName = "test-cat-" + java.util.UUID.randomUUID();
        String catJson = "[{\"name\":\"" + categoryName + "\"}]";

        String catResponse = mockMvc.perform(post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix())
                        .content(catJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String catId = com.jayway.jsonpath.JsonPath.read(catResponse, "$.payload[0].id");

        String productJson = "[{\"name\":\"throw-nested-exception\",\"category\":{\"id\":\"" + catId + "\"}}]";

        // RuntimeException (501) wraps IllegalStateException (409)
        // Should pick 409 because it's innermost
        mockMvc.perform(post(SpeedyConstants.URI + "/Product/" + SpeedyEndpoint.CREATE.suffix())
                        .content(productJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(409))
                .andExpect(jsonPath("$.message").value("Resource conflict detected"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // -- US3: Default Mappings (Zero Config) --

    @Test
    void defaultMapping_malformedJson_returns400() throws Exception {
        String malformedBody = "{ \"broken\": ";

        mockMvc.perform(post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.QUERY.suffix())
                        .content(malformedBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void defaultMapping_unknownEntity_returns400() throws Exception {
        String body = "{\"$from\":\"NonExistentEntity\"}";

        mockMvc.perform(post(SpeedyConstants.URI + "/NonExistentEntity/" + SpeedyEndpoint.QUERY.suffix())
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(not(emptyString())))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void defaultMapping_duplicateName_returns400() throws Exception {
        String categoryName = "dup-cat-" + java.util.UUID.randomUUID();
        String catJson = "[{\"name\":\"" + categoryName + "\"}]";

        mockMvc.perform(post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix())
                        .content(catJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix())
                        .content(catJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void defaultMapping_genericException_returns500WithMaskedMessage() throws Exception {
        String catJson = "[{\"name\":\"generic-error-trigger\"}]";

        mockMvc.perform(post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix())
                        .content(catJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Internal Server Error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void errorResponse_hasStandardJsonFormat() throws Exception {
        String categoryName = "dup-cat-" + java.util.UUID.randomUUID();
        String catJson = "[{\"name\":\"" + categoryName + "\"}]";

        mockMvc.perform(post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix())
                        .content(catJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix())
                        .content(catJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(not(emptyString())))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }
}
