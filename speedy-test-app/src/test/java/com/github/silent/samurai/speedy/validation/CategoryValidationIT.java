package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryValidationIT {

    @Autowired
    MockMvc mockMvc;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
    }

    @Test
    @DisplayName("CREATE should fail when custom validator rejects empty name")
    void createCategory_invalidName_shouldReturnBadRequest() {
        client.create("Category")
                .field("name", "")
                .execute()
                .expectBadRequest();
    }

    @Test
    @DisplayName("Valid CREATE followed by UPDATE and DELETE should succeed")
    void createUpdateDelete_validFlow() {
        SpeedyTestResult createResult = client.create("Category")
                .field("name", "it-category-1")
                .execute()
                .expectOk();

        String id = createResult.jsonPath("$.payload[0].id");

        client.update("Category")
                .key("id", id)
                .field("name", "it-category-updated")
                .execute()
                .expectOk();

        client.delete("Category")
                .key("id", id)
                .execute()
                .expectOk();
    }
}
