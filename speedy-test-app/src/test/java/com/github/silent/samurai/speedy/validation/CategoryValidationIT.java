package com.github.silent.samurai.speedy.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that start the Spring context (MockMvc) and hit the
 * Speedy REST endpoints to verify that custom validation defined in
 * {@link SpeedyValidation} is actually enforced during CREATE and that the
 * full CREATE → UPDATE → DELETE workflow works when the data is valid.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryValidationIT {

    @Autowired
    MockMvc mockMvc;

    private SpeedyClient<ResultActions> client;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        client = SpeedyClient.mockMvc(mockMvc);
    }

    @Test
    @DisplayName("CREATE should fail when custom validator rejects empty name")
    void createCategory_invalidName_shouldReturnBadRequest() throws Exception {
        client.create("Category")
                .addField("name", "") // empty name triggers custom validator failure
                .execute()
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Valid CREATE followed by UPDATE and DELETE should succeed")
    void createUpdateDelete_validFlow() throws Exception {
        // --- CREATE ---------------------------------------------------------
        ResultActions createAct = client.create("Category")
                .addField("name", "it-category-1")
                .execute()
                .andExpect(status().isOk());

        String createContent = createAct.andReturn().getResponse().getContentAsString();
        JsonNode createJson = mapper.readTree(createContent);
        String id = createJson.at("/payload/0/id").asText();

        // --- UPDATE ---------------------------------------------------------
        client.update("Category")
                .key("id", id)
                .field("name", "it-category-updated")
                .execute()
                .andExpect(status().isOk());

        // --- DELETE ---------------------------------------------------------
        client.delete("Category")
                .key("id", id)
                .execute()
                .andExpect(status().isOk());
    }
}
