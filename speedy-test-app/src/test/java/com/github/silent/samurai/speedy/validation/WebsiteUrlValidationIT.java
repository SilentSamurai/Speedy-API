package com.github.silent.samurai.speedy.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.ResultActions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests verifying UrlRule via AnnotatedPerson entity.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class WebsiteUrlValidationIT {

    @Autowired
    MockMvc mockMvc;

    private SpeedyClient<ResultActions> client;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        client = SpeedyClient.mockMvc(mockMvc);
    }

    @Nested
    @DisplayName("CREATE website URL validation")
    class CreateValidation {

        @Test
        @DisplayName("CREATE with invalid website should fail – UrlRule")
        void createInvalidWebsite_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "John")
                    .addField("age", 30)
                    .addField("email", "john@example.com")
                    .addField("code", "ABC12")
                    .addField("website", "not-a-url")
                    .execute()
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("website must be a valid URL")));
        }

        @Test
        @DisplayName("CREATE with valid website should succeed")
        void createValidWebsite_shouldSucceed() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "John")
                    .addField("age", 30)
                    .addField("email", "john@example.com")
                    .addField("code", "ABC12")
                    .addField("website", "https://example.com")
                    .execute()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload[0].id").exists());
        }
    }
}
