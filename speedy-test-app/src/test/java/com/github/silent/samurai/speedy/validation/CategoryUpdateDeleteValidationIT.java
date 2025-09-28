package com.github.silent.samurai.speedy.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests covering UPDATE & DELETE validation rules using MockMvc.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryUpdateDeleteValidationIT {

    @Autowired
    MockMvc mockMvc;

    private SpeedyClient<ResultActions> client;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        client = SpeedyClient.mockMvc(mockMvc);
    }

    @Nested
    @DisplayName("UPDATE validation")
    class UpdateValidation {
        @Test
        @DisplayName("UPDATE without key should fail with 400 Bad Request")
        void updateMissingKey_shouldFail() throws Exception {
            client.update("Category")
                    .field("name", "should-fail")
                    .execute()
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE validation")
    class DeleteValidation {
        @Test
        @DisplayName("DELETE without key should fail with 400 Bad Request")
        void deleteMissingKey_shouldFail() throws Exception {
            client.delete("Category")
                    .execute()
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Successful create & delete")
        void createThenDelete_shouldSucceed() throws Exception {
            // create valid entity
            ResultActions createAct = client.create("Category")
                    .addField("name", "it-cat-del")
                    .execute()
                    .andExpect(status().isOk());

            String content = createAct.andReturn().getResponse().getContentAsString();
            JsonNode json = mapper.readTree(content);
            String id = json.at("/payload/0/id").asText();

            // perform delete with key
            client.delete("Category")
                    .key("id", id)
                    .execute()
                    .andExpect(status().isOk());
        }
    }
}
