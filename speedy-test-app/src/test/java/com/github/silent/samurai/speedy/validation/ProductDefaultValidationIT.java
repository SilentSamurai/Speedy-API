package com.github.silent.samurai.speedy.validation;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that exercise the default validation pipeline for the Product entity
 * (no custom validators defined).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductDefaultValidationIT {

    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    MockMvc mockMvc;
    private SpeedyClient<ResultActions> client;
    private String categoryId;
    private String productId;

    @BeforeEach
    void init() throws Exception {
        client = SpeedyClient.mockMvc(mockMvc);

        // ensure a category exists – used as FK for products
        String uniqueCat = "it-cat-" + java.util.UUID.randomUUID();
        ResultActions catAct = client.create("Category")
                .addField("name", uniqueCat)
                .execute()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[0].id").exists())
                .andExpect(jsonPath("$.payload[0].id").isString());
        String catJson = catAct.andReturn().getResponse().getContentAsString();
        categoryId = mapper.readTree(catJson).at("/payload/0/id").asText();
    }

    @Nested
    @DisplayName("CREATE default validation")
    class CreateValidation {
        @Test
        @DisplayName("CREATE without required name should fail")
        void createMissingName_shouldFail() throws Exception {
            client.create("Product")
                    .addField("category.id", categoryId) // association ok
                    .execute()
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("CREATE valid product should succeed")
        void createValidProduct_shouldSucceed() throws Exception {
            ResultActions act = client.create("Product")
                    .addField("name", "it-prod-" + java.util.UUID.randomUUID())
                    .addField("category.id", categoryId)
                    .execute()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload[0].id").exists())
                    .andExpect(jsonPath("$.payload[0].id").isString());
        }
    }

    @Nested
    @DisplayName("UPDATE & DELETE default validation")
    class UpdateDeleteValidation {

        @Test
        @DisplayName("UPDATE partial payload succeeds (default validator)")
        void updatePartial_shouldSucceed() throws Exception {
            // create first
            MvcResult act = client.create("Product")
                    .addField("name", "it-prod-" + UUID.randomUUID())
                    .addField("category.id", categoryId)
                    .execute()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload[0].id").exists())
                    .andExpect(jsonPath("$.payload[0].id").isString())
                    .andReturn();

            String catJson = act.getResponse().getContentAsString();
            String id = mapper.readTree(catJson).at("/payload/0/id").asText();

            // partial update (only description)
            client.update("Product")
                    .key("id", id)
                    .field("description", "updated desc")
                    .execute()
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE without key fails (default validator)")
        void deleteWithoutKey_shouldFail() throws Exception {
            client.delete("Product")
                    .execute()
                    .andExpect(status().isBadRequest());
        }
    }
}
