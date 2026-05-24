package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class WebsiteUrlValidationIT {

    @Autowired
    MockMvc mockMvc;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
    }

    @Nested
    @DisplayName("CREATE website URL validation")
    class CreateValidation {

        @Test
        @DisplayName("CREATE with invalid website should fail – UrlRule")
        void createInvalidWebsite_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("website", "not-a-url")
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("website must be a valid URL"));
        }

        @Test
        @DisplayName("CREATE with valid website should succeed")
        void createValidWebsite_shouldSucceed() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("website", "https://example.com")
                    .execute()
                    .expectOk()
                    .expectJsonPathExists("$.payload[0].id");
        }
    }
}
