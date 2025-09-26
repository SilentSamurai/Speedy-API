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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests verifying validation for {@code AnnotatedPerson} entity
 * using both Speedy custom annotations and Jakarta Bean Validation annotations.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class AnnotatedPersonValidationIT {

    @Autowired
    MockMvc mockMvc;

    private SpeedyClient<ResultActions> client;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        client = SpeedyClient.mockMvc(mockMvc);
    }

    @Nested
    @DisplayName("CREATE validation")
    class CreateValidation {
        @Test
        @DisplayName("CREATE invalid name (blank) should fail – NotBlankRule")
        void createBlankName_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "   ")
                    .addField("age", 25)
                    .addField("email", "john@example.com")
                    .addField("code", "ABC12")
                    .execute()
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("CREATE invalid age (<18) should fail – MinRule")
        void createUnderage_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "John")
                    .addField("age", 15)
                    .addField("email", "john@example.com")
                    .addField("code", "ABC12")
                    .execute()
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("age must be >= 18")));
        }

        @Test
        @DisplayName("CREATE invalid email format should fail – EmailRule")
        void createInvalidEmail_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "John")
                    .addField("age", 30)
                    .addField("email", "not-an-email")
                    .addField("code", "ABC12")
                    .execute()
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("email must be a valid email")));
        }

        @Test
        @DisplayName("CREATE name too short (<3) should fail – LengthRule min")
        void createShortName_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "Jo") // 2 chars – below min length 3
                    .addField("age", 30)
                    .addField("email", "john@example.com")
                    .addField("code", "ABC12")
                    .execute()
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("CREATE name too long (>20) should fail – LengthRule max")
        void createLongName_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "ThisNameIsWayTooLongForTheField")
                    .addField("age", 30)
                    .addField("email", "john@example.com")
                    .addField("code", "ABC12")
                    .execute()
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("CREATE age above 60 should fail – MaxRule")
        void createOverage_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "John")
                    .addField("age", 65) // above max 60
                    .addField("email", "john@example.com")
                    .addField("code", "ABC12")
                    .execute()
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("age must be <= 60")));
        }

        @Test
        @DisplayName("CREATE invalid code pattern should fail – RegexRule")
        void createInvalidCode_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "John")
                    .addField("age", 30)
                    .addField("email", "john@example.com")
                    .addField("code", "ab12") // invalid pattern
                    .execute()
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("does not match pattern")));
        }

        @Test
        @DisplayName("CREATE valid payload should succeed")
        void createValid_shouldSucceed() throws Exception {
            ResultActions act = client.create("AnnotatedPerson")
                    .addField("name", "John Doe")
                    .addField("age", 30)
                    .addField("email", "john.doe@example.com")
                    .addField("code", "ABC12")
                    .execute()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload[0].id").exists());

            // store ID for subsequent tests
            String body = act.andReturn().getResponse().getContentAsString();
            JsonNode json = mapper.readTree(body);
            String id = json.at("/payload/0/id").asText();

            // --- UPDATE ------------------------------------------------------
            client.update("AnnotatedPerson")
                    .key("id", id)
                    .field("name", "Jane Doe")
                    .execute()
                    .andExpect(status().isOk());

            // --- DELETE ------------------------------------------------------
            client.delete("AnnotatedPerson")
                    .key("id", id)
                    .execute()
                    .andExpect(status().isOk());
        }
    }
}
