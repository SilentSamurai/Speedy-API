package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryUpdateDeleteValidationIT {

    @Autowired
    MockMvc mockMvc;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
    }

    @Nested
    @DisplayName("UPDATE validation")
    class UpdateValidation {
        @Test
        @DisplayName("UPDATE without key should fail with 400 Bad Request")
        void updateMissingKey_shouldFail() {
            client.update("Category")
                    .field("name", "should-fail")
                    .execute()
                    .expectBadRequest();
        }
    }

    @Nested
    @DisplayName("DELETE validation")
    class DeleteValidation {
        @Test
        @DisplayName("DELETE without key should fail with 400 Bad Request")
        void deleteMissingKey_shouldFail() {
            client.delete("Category")
                    .execute()
                    .expectBadRequest();
        }

        @Test
        @DisplayName("Successful create & delete")
        void createThenDelete_shouldSucceed() {
            SpeedyTestResult createResult = client.create("Category")
                    .field("name", "it-cat-del")
                    .execute()
                    .expectOk();

            String id = createResult.jsonPath("$.payload[0].id");

            client.delete("Category")
                    .key("id", id)
                    .execute()
                    .expectOk();
        }
    }
}
