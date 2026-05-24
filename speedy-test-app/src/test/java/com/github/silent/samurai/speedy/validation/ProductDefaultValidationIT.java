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

import java.util.UUID;

import static org.hamcrest.Matchers.isA;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductDefaultValidationIT {

    @Autowired
    MockMvc mockMvc;
    private SpeedyTest client;
    private String categoryId;

    @BeforeEach
    void init() throws Exception {
        client = SpeedyTest.mockMvc(mockMvc);

        String uniqueCat = "it-cat-" + java.util.UUID.randomUUID();
        SpeedyTestResult catResult = client.create("Category")
                .field("name", uniqueCat)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].id");
        categoryId = catResult.jsonPath("$.payload[0].id");
    }

    @Nested
    @DisplayName("CREATE default validation")
    class CreateValidation {
        @Test
        @DisplayName("CREATE without required name should fail")
        void createMissingName_shouldFail() throws Exception {
            client.create("Product")
                    .field("category.id", categoryId)
                    .execute()
                    .expectBadRequest();
        }

        @Test
        @DisplayName("CREATE valid product should succeed")
        void createValidProduct_shouldSucceed() throws Exception {
            client.create("Product")
                    .field("name", "it-prod-" + java.util.UUID.randomUUID())
                    .field("category.id", categoryId)
                    .execute()
                    .expectOk()
                    .expectJsonPathExists("$.payload[0].id");
        }
    }

    @Nested
    @DisplayName("UPDATE & DELETE default validation")
    class UpdateDeleteValidation {

        @Test
        @DisplayName("UPDATE partial payload succeeds (default validator)")
        void updatePartial_shouldSucceed() throws Exception {
            SpeedyTestResult createResult = client.create("Product")
                    .field("name", "it-prod-" + UUID.randomUUID())
                    .field("category.id", categoryId)
                    .execute()
                    .expectOk()
                    .expectJsonPathExists("$.payload[0].id");

            String id = createResult.jsonPath("$.payload[0].id");

            client.update("Product")
                    .key("id", id)
                    .field("description", "updated desc")
                    .execute()
                    .expectOk();
        }

        @Test
        @DisplayName("DELETE without key fails (default validator)")
        void deleteWithoutKey_shouldFail() throws Exception {
            client.delete("Product")
                    .execute()
                    .expectBadRequest();
        }
    }
}
